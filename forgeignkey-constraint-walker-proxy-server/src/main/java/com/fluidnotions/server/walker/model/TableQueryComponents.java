package com.fluidnotions.server.walker.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TableQueryComponents {

	private static final Log log = LogFactory
			.getLog(TableQueryComponents.class);

	private boolean target;
	private String tableName;
	private String dateConditionStmt;
	private String queryType;
	private Map<String, String> whereColEqsMap;
	private Map<String, LinkedHashSet<String>> whereColInArrMap;
	
	private boolean compositeForeignKeyConstraint;
	private boolean strictCompositeForeignKeyConditions;
	//need a list so that duplicate values can be supported in deleteWalker InArr
	//for strictCompositeForeignKeyConditions=true
	private Map<String, LinkedList<String>> whereColInArrListMap;
	
	private String selectCols = "*";
	private int order;

	public TableQueryComponents(String tableName) {
		this(tableName, false);
	}

	public TableQueryComponents(String tableName, boolean target) {
		super();
		
		this.tableName = tableName;
		this.target = target;

		this.whereColEqsMap = new LinkedHashMap<String, String>();
		this.whereColInArrMap = new LinkedHashMap<String, LinkedHashSet<String>>();
		this.whereColInArrListMap = new LinkedHashMap<String, LinkedList<String>>();
		this.queryType = "select";
	}

	public TableQueryModel buildTableQueryModel() {
		TableQueryModel tqm = null;
		try {
			tqm = new TableQueryModel(this.order, this.tableName,
					this.toSqlQueryString());
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		
		return tqm;
	}

	public String getTableName() {
		return tableName;
	}

	public Map<String, String> getWhereColEqsMap() {
		return whereColEqsMap;
	}

	public Map<String, LinkedHashSet<String>> getWhereColInArrMap() {
		return whereColInArrMap;
	}

	public Map<String, LinkedList<String>> getWhereColInArrListMap() {
		return whereColInArrListMap;
	}

	public void addWhereColEqsToMap(String col, String value) {
		this.whereColEqsMap.put(col, value);
	}

	public void addWhereColInArrToMap(String col, Set<String> sqlStmArrSet) {
		this.whereColInArrMap.put(col, (LinkedHashSet<String>) sqlStmArrSet);
		
	}
	
	public void addWhereColInArrListToMap(String col, List<String> sqlStmArrList) {
		this.whereColInArrListMap.put(col, (LinkedList<String>) sqlStmArrList);
		
	}
	
	

	public void setSelectCols(String selectCols) {
		this.selectCols = selectCols;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	// one value is handled difference to an arr/set, but when consolidating
	// we need unified access to col->values map even is the values
	// list has a size of one - it's simpler to handle this internally
	public Map<String, LinkedHashSet<String>> getWhereColCombinedMap() {
		Map<String, LinkedHashSet<String>> whereColCombinedMap = new LinkedHashMap<String, LinkedHashSet<String>>(
				this.whereColInArrMap);
		for (Map.Entry<String, String> e : this.whereColEqsMap.entrySet()) {
			if (whereColCombinedMap.containsKey(e.getKey())) {
				whereColCombinedMap.get(e.getKey()).add(e.getValue());
			} else {
				LinkedHashSet<String> vset = new LinkedHashSet<String>();
				vset.add(e.getValue());
				whereColCombinedMap.put(e.getKey(), vset);
			}
		}
		return whereColCombinedMap;
	}

	public String toSqlQueryString() {

		String sql = this.queryType
				+ " "
				+ this.selectCols
				+ " from "
				+ this.tableName
				+ (this.dateConditionStmt != null ? " where "
						+ this.dateConditionStmt : "");
		if (this.whereColEqsMap.size() > 0 || this.whereColInArrMap.size() > 0) {
			sql += " where ";
		}
		boolean whereAdded = false;
		for (Map.Entry<String, String> e : this.whereColEqsMap.entrySet()) {
			whereAdded = true;
			sql += e.getKey() + " = " + "\"" + e.getValue() + "\"" + " and ";
		}
		if (!strictCompositeForeignKeyConditions) {
			for (Entry<String, LinkedHashSet<String>> e : this.whereColInArrMap
					.entrySet()) {
				whereAdded = true;
				String arr = "(";
				// "CALENDAR_STATUS" , ))
				for (String s : e.getValue()) {
					arr += "\"" + s + "\"" + " , ";
				}
				arr = arr.substring(0, arr.lastIndexOf(" , "));
				arr += ")";
				sql += e.getKey() + " in " + arr + " and ";
			}
			if (whereAdded && sql.indexOf(" and ") != -1)
				sql = sql.substring(0, sql.lastIndexOf(" and "));
		} else {
			//example:
			//current:  PARTY_ID in ("FEDEX" , "Company" , "DHL" , "UPS" , "_NA_") and SHIPMENT_METHOD_TYPE_ID in ("GROUND_HOME" , "SECOND_DAY" , ...)
			//desired: (PARTY_ID = "FEDEX" and SHIPMENT_METHOD_TYPE_ID = "GROUND_HOME") or (PARTY_ID = "DHL" and SHIPMENT_METHOD_TYPE_ID = "SECOND_DAY")
			List<String> keysList = new ArrayList<String>(this.whereColInArrListMap.keySet());
			int keysListSize = keysList.size();
			int inArrMappedSize = retrieveWhereColInArrListMappedSize();
			if (keysListSize>0) {
				//debug
				/*for(Map.Entry<String, LinkedList<String>> e: this.whereColInArrListMap.entrySet()){
					String v = "";
					for(String s: e.getValue()){
						v += ","+(s!=null?s:"null");
					}
					
					log.debug(e.getKey()+" --> "+v);
				}*/
				
				if (sql.indexOf(" where ") == -1) {
					sql += " where ";
				}
				for (int x = 0; x < inArrMappedSize; x++) {
					sql += " (";
					for (int y = 0; y < keysListSize; y++) {
						sql += keysList.get(y)
								+ " = "
								+ "\""
								+ retrieveIndexValueInMappedList(
										keysList.get(y), x) + "\"" + " and ";
					}
					if (sql.indexOf(" and ") != -1)
						sql = sql.substring(0, sql.lastIndexOf(" and "));
					sql += ") or ";
				}
				if (sql.indexOf(" or ") != -1)
					sql = sql.substring(0, sql.lastIndexOf(" or"));
				//log.debug("strict composite sql: " + sql);
			}
		}
		return sql;
	}
	
	private String retrieveIndexValueInMappedList(String key, int index){
		return this.whereColInArrListMap.get(key).get(index);
	}
	
	private String retrieveIndexValueInMappedSet(String key, int setIndex){
		return new ArrayList<String>(this.whereColInArrMap.get(key)).get(setIndex);
	}
	
	private int retrieveWhereColInArrListMappedSize(){
		int size = 0;
		for(Map.Entry<String, LinkedList<String>> e: this.whereColInArrListMap.entrySet()){
			int s = e.getValue().size();
			if(s>size){
				size = s;
			}
			//log.debug(e.getKey()+"-> list size: "+s);
		}
		return size;
	}

	public boolean isTarget() {
		return target;
	}

	public void setTarget(boolean target) {
		this.target = target;
	}

	public boolean isCompositeForeignKeyConstraint() {
		return compositeForeignKeyConstraint;
	}

	public void setCompositeForeignKeyConstraint(
			boolean compositeForeignKeyConstraint) {
		this.compositeForeignKeyConstraint = compositeForeignKeyConstraint;
	}

	public String getDateConditionStmt() {
		return dateConditionStmt;
	}

	public void setDateConditionStmt(String dateConditionStmt) {
		this.dateConditionStmt = dateConditionStmt;
	}

	public String getQueryType() {
		return queryType;
	}

	public void setQueryType(String queryType) {
		this.queryType = queryType;
	}

	public boolean isStrictCompositeForeignKeyConditions() {
		return strictCompositeForeignKeyConditions;
	}

	public void setStrictCompositeForeignKeyConditions(
			boolean strictCompositeForeignKeyConditions) {
		this.strictCompositeForeignKeyConditions = strictCompositeForeignKeyConditions;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + order;
		result = prime * result
				+ ((tableName == null) ? 0 : tableName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TableQueryComponents other = (TableQueryComponents) obj;
		if (order != other.order)
			return false;
		if (tableName == null) {
			if (other.tableName != null)
				return false;
		} else if (!tableName.equals(other.tableName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TableQueryComponents [target=" + target + ", tableName="
				+ tableName + ", dateConditionStmt=" + dateConditionStmt
				+ ", queryType=" + queryType
				+ ", compositeForeignKeyConstraint="
				+ compositeForeignKeyConstraint
				+ ", strictCompositeForeignKeyConditions="
				+ strictCompositeForeignKeyConditions + ", selectCols="
				+ selectCols + ", order=" + order + "]";
	}
	
	
}
