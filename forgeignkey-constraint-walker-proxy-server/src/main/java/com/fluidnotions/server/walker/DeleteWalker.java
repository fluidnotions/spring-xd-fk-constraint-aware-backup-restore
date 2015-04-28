package com.fluidnotions.server.walker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.xd.tuple.Tuple;

import com.fluidnotions.server.walker.database.DatabasesMetaDataMap;
import com.fluidnotions.server.walker.database.TupleRowMapper;
import com.fluidnotions.server.walker.model.TableQueryComponents;
import com.fluidnotions.server.walker.model.TableQueryModel;

@Service
public class DeleteWalker {

	private static final Log log = LogFactory.getLog(DeleteWalker.class);

	@Autowired
	private DatabasesMetaDataMap databasesMetaDataMap;
	
	
	 private  JdbcTemplate jdbcTemplate;
	 private Database databaseMetadata;
	
	private int stepCycleCount, walkCycleCount = 0;

	private List<TableQueryComponents> queryList;
	private Set<String> toSqlCyclicCheck;


	public DeleteWalker() {	
		queryList = new ArrayList<TableQueryComponents>();
		toSqlCyclicCheck = new LinkedHashSet <String>();
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
	
	public List<TableQueryComponents> run(String xdJobDefName, TableQueryComponents sqlQuery){
		clear();
		this.jdbcTemplate = databasesMetaDataMap.databaseMetaData(xdJobDefName)
				.getJdbcTemplate();
		this.databaseMetadata = databasesMetaDataMap.databaseMetaData(
				xdJobDefName).getMetaData();
		try {
			walk(sqlQuery);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getQueryList();
	}

	
	private void walk(TableQueryComponents sqlQuery) throws Exception {
		walkCycleCount++;
		
		addToFinalQueryList(sqlQuery);
		
		for(TableQueryComponents qr : step(sqlQuery)) {
			walk(qr);
		}
		//log.debug("walkCycleCount: "+walkCycleCount+" stepCycleCount: "+stepCycleCount);

	}

	private synchronized void addToFinalQueryList(TableQueryComponents sqlQuery) {
		sqlQuery.setOrder(queryList.size()+1);
		queryList.add(sqlQuery);
		
	}
	
	private List<TableQueryComponents> step(TableQueryComponents sqlQuery) throws Exception {
		stepCycleCount++;
		List<TableQueryComponents> childSelectStms = new ArrayList<TableQueryComponents>();
		List<Tuple> resultSet = jdbcTemplate.query(sqlQuery.toSqlQueryString(), new TupleRowMapper());
		log.debug("table name: "+sqlQuery.getTableName()+" result set size: "+resultSet.size());
		//if the the result set is empty it's pointless adding it, an empty table may have parents but without criteria
		//we'll end up with all records
		if(resultSet.isEmpty()){
			//return an empty result set no ForeignKeyConstraints to parent tables are relevant to our case
			//it's already been added to queryList so we need to remove it
			queryList.remove(sqlQuery);
			return childSelectStms;
		}
		
		String targetTableName = sqlQuery.getTableName();
		Table targetTable = this.databaseMetadata.getTablesByName().get(targetTableName);
		log.debug("targetTable: "+targetTable);
		List<TableColumn> columns = targetTable.getColumns();
		
		Collection<ForeignKeyConstraint> foreignKeyConstraintsOfChildrenTables = new LinkedHashSet <ForeignKeyConstraint>();
		for(TableColumn tc: columns){
			Set<TableColumn> children = tc.getChildren();
			if(children.size()>0){
				for(TableColumn ctc: children){
					ForeignKeyConstraint childConst = tc.getChildConstraint(ctc);
					foreignKeyConstraintsOfChildrenTables.add(childConst);
				}
			}
		}
		//debug
		/*log.debug("Child tables:-");
		for(ForeignKeyConstraint fkc: foreignKeyConstraintsOfChildrenTables){
			log.debug("Child table: "+fkc.getChildTable());
		}*/
		
		processFksForDelete(childSelectStms, resultSet, foreignKeyConstraintsOfChildrenTables);

		return childSelectStms;
	}

	
	private void processFksForDelete(
					List<TableQueryComponents> childSelectStms, List<Tuple> resultSet,
					Collection<ForeignKeyConstraint> fkcs) {
				for (ForeignKeyConstraint fkc : fkcs) {
					//a table might have many ForeignKeyConstraint but it is unlikely and will point to the same parent table
					//it's still possible I'm guessing and I'm unsure if such a case is handled
					Table childTable = fkc.getChildTable();
					TableQueryComponents childTableSelectQuery = new TableQueryComponents(childTable.getName());
					//log.debug("--------------------------------------------------------------------------------");
					//log.debug("ForeignKeyConstraint: "+fkc.toString());
					
					//these 2 lists are parallel, but they are also are for one ForeignKeyConstraint
					List<TableColumn> parentColumnsRefByThisConstraint = fkc
							.getParentColumns();
					List<TableColumn> childColumnsRefByThisConstraint = fkc
							.getChildColumns();

					int plen = parentColumnsRefByThisConstraint.size();
					int clen = childColumnsRefByThisConstraint.size();
					assert (plen == clen);
					
					 //the only time this loop will cycle more then once is 
					//when we are dealing with a composite ForeignKeyConstraint
					//in such a case col1 IN (v1, v2, v3) and col2 in (v4, v5, v6)
					//will allow mixing of fk combinations this is more of an issue 
					//in delete operations
					if(clen>1){
						//mainly this is about how toSqlQueryString() is implemented
						//in the case of deletion (select initially) queries
						childTableSelectQuery.setCompositeForeignKeyConstraint(true);
						childTableSelectQuery.setStrictCompositeForeignKeyConditions(true);
					}

						for (int i = 0; i < clen; i++) {
							TableColumn childCol = childColumnsRefByThisConstraint
									.get(i);
							//log.debug("childCol: "+childCol.toString());
							TableColumn parentCol = parentColumnsRefByThisConstraint
									.get(i);
							//log.debug("parentCol: "+parentCol.toString());
							// use set to remove duplicate values
							Set<String> sqlStmArrSet = new LinkedHashSet <String>();
							List<String> sqlStmArrList = new LinkedList <String>();
							for(Tuple t: resultSet){
								String value =  t.getString(parentCol.getName());
								//if we are using null values are preserved to insure set ordering
								//maintains composite
								if(childTableSelectQuery.isStrictCompositeForeignKeyConditions()){
									//sqlStmArrSet.add(value);
									sqlStmArrList.add(value);	
								}else{
									if (value != null) sqlStmArrSet.add(value);
								}
								
							}
							
							if(childTableSelectQuery.isStrictCompositeForeignKeyConditions()){
								
								childTableSelectQuery.addWhereColInArrListToMap(childCol.getName(), sqlStmArrList);
								
							}else{
								
								if (sqlStmArrSet.size() == 1) {
									childTableSelectQuery.addWhereColEqsToMap(childCol.getName(), sqlStmArrSet.iterator().next());
								} else if (sqlStmArrSet.size() > 1) {
									childTableSelectQuery.addWhereColInArrToMap(childCol.getName(), sqlStmArrSet);
								}
								
							}

							

						}
						
						String toSql = childTableSelectQuery.toSqlQueryString();
						//ensures we don't go round in circles
						//parentTableSelectQuery.getWhereColEqsMap().size() = 0 indicates the presents of ignored null values, these should not cause constraint violations 
						//if left out
						if((childTableSelectQuery.getWhereColEqsMap().size()>0 || childTableSelectQuery.getWhereColInArrMap().size()>0 || childTableSelectQuery.getWhereColInArrListMap().size()>0) && this.toSqlCyclicCheck.add(toSql) ){
							childSelectStms.add(childTableSelectQuery);
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
		//this is the last step so we change select to delete here
		for(TableQueryComponents tqc: queryList){
			tqc.setQueryType("delete");
			tqc.setSelectCols("");
		}
		return queryList;
	}

}
