package com.fluidnotions.server.walker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.ForeignKeyConstraint;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.model.TableColumn;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.xd.tuple.Tuple;

import com.fluidnotions.server.walker.database.DatabasesMetaDataMap;
import com.fluidnotions.server.walker.database.TupleRowMapper;
import com.fluidnotions.server.walker.model.TableQueryComponents;
import com.fluidnotions.server.walker.model.TableQueryModel;

@Service
public class SelectWalker {

	private static final Log log = LogFactory.getLog(SelectWalker.class);

	@Autowired
	private DatabasesMetaDataMap databasesMetaDataMap;

	private int stepCycleCount, walkCycleCount = 0;

	private boolean strictCompositeForeignKeyConditionsForSelect = false;

	private List<TableQueryComponents> queryList;
	private Set<String> toSqlCyclicCheck;

	private JdbcTemplate jdbcTemplate;
	private Database databaseMetadata;

	public SelectWalker() {
		queryList = new ArrayList<TableQueryComponents>();
		toSqlCyclicCheck = new HashSet<String>();
	}

	public List<TableQueryModel> retrieveCompleteTableNameList(
			String xdJobDefName) throws Exception {
		Set<String> tables = databasesMetaDataMap
				.databaseMetaData(xdJobDefName).getMetaData().getTablesByName()
				.keySet();
		List<TableQueryModel> tqms = new ArrayList<TableQueryModel>();
		for (String t : tables) {
			tqms.add(new TableQueryModel(0, t, null));
		}
		return tqms;
	}

	// if you are going to consolidate queries for same table, table load order
	// needs to be created first
	// 0 is to be the last loaded the highest number first. The number for a
	// table must always be the max
	// before consolidation
	public List<TableQueryComponents> consolidateTablesQueries(
			List<TableQueryComponents> all) {
		List<TableQueryComponents> consolidated = new ArrayList<TableQueryComponents>();

		// consolidation does not
		// handlestrictCompositeForeignKeyConditionsForSelect=true
		// it could be extended and would just not consolidate
		// TableQueryComponents where
		// compositeForeignKeyConstraint=true but would consolidate normally for
		// others
		if (strictCompositeForeignKeyConditionsForSelect) {
			consolidated.addAll(all);
		} else {
			// get table name set
			Set<String> tableNameSet = new HashSet<String>();
			for (TableQueryComponents tsq : all) {
				tableNameSet.add(tsq.getTableName());
			}
			// handle table name group
			for (String tableName : tableNameSet) {
				List<TableQueryComponents> tableNameGroup = new ArrayList<TableQueryComponents>();
				for (TableQueryComponents tsq : all) {
					if (tableName.equals(tsq.getTableName())) {
						tableNameGroup.add(tsq);
					}
				}
				if (tableNameGroup.size() == 1) {
					log.debug("one table group (" + tableName
							+ ") will continue ..");
					consolidated.add(tableNameGroup.get(0));
					continue;
				}

				// debug
				int c = 1;
				for (TableQueryComponents tsq : tableNameGroup) {

					if (tsq.isTarget()) {
						log.debug("tableNameGroup target table #" + c++ + " : "
								+ tsq.buildTableQueryModel().toString());
					}
				}

				// also check if any are marked as target, the dateConditionStmt
				// needs to be retrieved and set into the consolidation object
				TableQueryComponents targetTableQueryComponents = null;
				// target tables need to be excluded from consolidation
				for (TableQueryComponents tsq : tableNameGroup) {

					if (tsq.isTarget()
							&& (tsq.getWhereColEqsMap().size() == 0 && tsq
									.getWhereColInArrMap().size() == 0)) {
						targetTableQueryComponents = tsq;
						break;
					}
				}
				// remove target from group will be added to consolidated list
				// after other non-targets have been consolidated
				// we are assuming there is only one target of a table name per
				// group which may not always be the cases but
				// there should only be one without where values
				if (targetTableQueryComponents != null)
					tableNameGroup.remove(targetTableQueryComponents);

				// work within table group
				// find highest order
				int highest = 0;
				// log.debug("tableNameGroup.size: "+tableNameGroup.size()+"| tableName: "+tableName+", tableNameGroup.get(0).getTableName: "+tableNameGroup.get(0).getTableName());

				for (TableQueryComponents tsq : tableNameGroup) {
					// log.debug("order: "+tsq.getOrder());
					if (tsq.getOrder() > highest) {
						highest = tsq.getOrder();
						// log.debug("order: "+highest);
					}
				}
				// log.debug("highest: "+highest);
				// get set of unique col names in tableNameGroup
				Set<String> colNames = new HashSet<String>();
				for (TableQueryComponents tsq : tableNameGroup) {
					for (String colKey : tsq.getWhereColCombinedMap().keySet()) {
						// log.debug("WhereColCombinedMap: key(colKey): "+colKey);
						colNames.add(colKey);
					}
				}
				// IN (...) are already OR type WHERE's we only need to worry
				// about AND for composite fks, when we consolidate unique col
				// names
				// but all it means is there will be a couple of extra records
				// not a major problem for date range backups
				HashMap<String, Set<String>> consolidatedWhereColCombinedMap = new HashMap<String, Set<String>>();
				for (String colname : colNames) {
					for (TableQueryComponents tsq : tableNameGroup) {
						for (Entry<String, LinkedHashSet<String>> e : tsq
								.getWhereColCombinedMap().entrySet()) {
							if (colname.equals(e.getKey())) {
								if (consolidatedWhereColCombinedMap
										.containsKey(colname)) {
									consolidatedWhereColCombinedMap
											.get(colname).addAll(e.getValue());
								} else {
									consolidatedWhereColCombinedMap.put(
											colname, e.getValue());
								}
							}
						}
					}
				}
				TableQueryComponents consolidatedTableSelectQuery = new TableQueryComponents(
						tableName);
				/*
				 * if(dateConditionStmt!=null){
				 * consolidatedTableSelectQuery.setDateConditionStmt
				 * (dateConditionStmt); }
				 */
				// set order to highest - the highest is the first loaded
				consolidatedTableSelectQuery.setOrder(highest);
				// add back into highestOrderTableSelectQuery if a value set
				// only contains 1 elements add it to appropriate map
				for (Map.Entry<String, Set<String>> e : consolidatedWhereColCombinedMap
						.entrySet()) {
					if (e.getValue().size() == 1) {
						consolidatedTableSelectQuery.addWhereColEqsToMap(
								e.getKey(), e.getValue().iterator().next());
					} else {
						consolidatedTableSelectQuery.addWhereColInArrToMap(
								e.getKey(), e.getValue());
					}

				}
				consolidated.add(consolidatedTableSelectQuery);
				// target table removed from tableNameGroup before consolidation
				// is now added back
				if (targetTableQueryComponents != null)
					consolidated.add(targetTableQueryComponents);

			}
		}

		return consolidated;
	}

	public List<TableQueryComponents> run(String xdJobDefName,
			List<TableQueryModel> sqlQueryList) {
		clear();
		this.jdbcTemplate = databasesMetaDataMap.databaseMetaData(xdJobDefName)
				.getJdbcTemplate();
		this.databaseMetadata = databasesMetaDataMap.databaseMetaData(
				xdJobDefName).getMetaData();
		for (TableQueryModel tqm : sqlQueryList) {
			try {

				walk(tqm.buildTableQueryComponentsObj(true));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return getQueryList();
	}

	private void walk(TableQueryComponents sqlQuery) throws Exception {
		walkCycleCount++;

		addToFinalQueryList(sqlQuery);

		for (TableQueryComponents qr : step(sqlQuery)) {
			walk(qr);
		}
		// log.debug("walkCycleCount: "+walkCycleCount+" stepCycleCount: "+stepCycleCount);

	}

	private synchronized void addToFinalQueryList(TableQueryComponents sqlQuery) {
		sqlQuery.setOrder(queryList.size() + 1);
		queryList.add(sqlQuery);

	}

	private List<TableQueryComponents> step(TableQueryComponents sqlQuery)
			throws Exception {
		stepCycleCount++;
		List<TableQueryComponents> parentSelectStms = new ArrayList<TableQueryComponents>();
		List<Tuple> resultSet = jdbcTemplate.query(sqlQuery.toSqlQueryString(),
				new TupleRowMapper());
		log.debug("table name: " + sqlQuery.getTableName()
				+ " result set size: " + resultSet.size());
		// if the the result set is empty it's pointless adding it, an empty
		// table may have parents but without criteria
		// we'll end up with all records
		if (resultSet.isEmpty()) {
			// return an empty result set no ForeignKeyConstraints to parent
			// tables are relevant to our case
			// it's already been added to queryList so we need to remove it
			queryList.remove(sqlQuery);
			return parentSelectStms;
		}

		String targetTableName = sqlQuery.getTableName();
		Table targetTable = this.databaseMetadata.getTablesByName().get(
				targetTableName);
		// log.debug("targetTableName: "+targetTableName);
		Collection<ForeignKeyConstraint> fkcs = targetTable.getForeignKeys();
		processFksForSelect(parentSelectStms, resultSet, fkcs);

		return parentSelectStms;
	}

	private void processFksForSelect(
			List<TableQueryComponents> parentSelectStms, List<Tuple> resultSet,
			Collection<ForeignKeyConstraint> fkcs) {
		for (ForeignKeyConstraint fkc : fkcs) {
			// a table might have many ForeignKeyConstraint but it is unlikely
			// and will point to the same parent table
			// it's still possible I'm guessing and I'm unsure if such a case is
			// handled
			Table parentTable = fkc.getParentTable();
			TableQueryComponents parentTableSelectQuery = new TableQueryComponents(
					parentTable.getName());
			// log.debug("--------------------------------------------------------------------------------");
			// log.debug("parentTable: "+parentTable);
			// log.debug("childTable: "+fkc.getChildTable());
			// log.debug("ForeignKeyConstraint: "+fkc.toString());

			// these 2 lists are parallel, but they are also are for one
			// ForeignKeyConstraint
			List<TableColumn> parentColumnsRefByThisConstraint = fkc
					.getParentColumns();
			List<TableColumn> childColumnsRefByThisConstraint = fkc
					.getChildColumns();

			int plen = parentColumnsRefByThisConstraint.size();
			int clen = childColumnsRefByThisConstraint.size();
			assert (plen == clen);

			// the only time this loop will cycle more then once is
			// when we are dealing with a composite ForeignKeyConstraint
			// in such a case: col1 IN (v1, v2, v3) and col2 in (v4, v5, v6)
			if (clen > 1) {
				parentTableSelectQuery.setCompositeForeignKeyConstraint(true);
				if (strictCompositeForeignKeyConditionsForSelect) {
					parentTableSelectQuery
							.setStrictCompositeForeignKeyConditions(true);
				}
			}

			for (int i = 0; i < clen; i++) {
				TableColumn childCol = childColumnsRefByThisConstraint.get(i);
				// log.debug("childCol: "+childCol.toString());
				TableColumn parentCol = parentColumnsRefByThisConstraint.get(i);
				// log.debug("parentCol: "+parentCol.toString());
				// use set to remove duplicate values
				Set<String> sqlStmArrSet = new LinkedHashSet<String>();
				List<String> sqlStmArrList = new LinkedList<String>();
				for (Tuple t : resultSet) {
					String value = t.getString(childCol.getName());
					// if we are using null values are preserved to insure set
					// ordering
					// maintains composite
					if (parentTableSelectQuery
							.isStrictCompositeForeignKeyConditions()) {
						// sqlStmArrSet.add(value);
						sqlStmArrList.add(value);
					} else {
						if (value != null)
							sqlStmArrSet.add(value);
					}
				}

				if (parentTableSelectQuery
						.isStrictCompositeForeignKeyConditions()) {

					parentTableSelectQuery.addWhereColInArrListToMap(
							childCol.getName(), sqlStmArrList);

				} else {

					if (sqlStmArrSet.size() == 1) {
						parentTableSelectQuery.addWhereColEqsToMap(parentCol
								.getName(), sqlStmArrSet.iterator().next());
					} else if (sqlStmArrSet.size() > 1) {
						parentTableSelectQuery.addWhereColInArrToMap(
								parentCol.getName(), sqlStmArrSet);
					}

				}

			}

			String toSql = parentTableSelectQuery.toSqlQueryString();
			// ensures we don't go round in circles
			// parentTableSelectQuery.getWhereColEqsMap().size() = 0 indicates
			// the presents of ignored null values, these should not cause
			// constraint violations
			// if left out
			if ((parentTableSelectQuery.getWhereColEqsMap().size() > 0 || parentTableSelectQuery
					.getWhereColInArrMap().size() > 0)
					&& this.toSqlCyclicCheck.add(toSql)) {
				parentSelectStms.add(parentTableSelectQuery);
			}
		}
	}

	public int getToSqlCyclicCheckSize() {
		return toSqlCyclicCheck.size();
	}

	public void clear() {
		this.jdbcTemplate = null;
		this.databaseMetadata = null;
		stepCycleCount = 0;
		walkCycleCount = 0;
		this.toSqlCyclicCheck.clear();
		this.queryList.clear();
	}

	public List<TableQueryComponents> getQueryList() {
		return queryList;
	}

}
