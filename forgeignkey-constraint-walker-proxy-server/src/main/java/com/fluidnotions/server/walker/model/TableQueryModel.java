package com.fluidnotions.server.walker.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TableQueryModel {
	
	private static final Log log = LogFactory.getLog(TableQueryModel.class);

	private int order;
	private String tableName;
	private String query;
	private String beforeDate;
	private String afterDate;
	private boolean lastUpdated;
	private boolean created;

	public TableQueryModel() {
	}

	public TableQueryModel(int order, String tableName, String query) {
		super();
		this.order = order;
		this.tableName = tableName;
		this.query = query;
	}
	
	public Map<String, String> toRelationalDataPortJobParamMap(){
		Map<String, String> insert = new HashMap<String, String>();
	
		insert.put("FILE_NAME_PREFIX", this.order+"");
		insert.put("FILE_NAME", this.tableName);
		insert.put("SQL_QUERY", this.query);
		
		return insert;
	}

	public TableQueryComponents buildTableQueryComponentsObj() {
		return buildTableQueryComponentsObj(false);
	}

	public TableQueryComponents buildTableQueryComponentsObj(boolean target) {
		TableQueryComponents tqc = new TableQueryComponents(this.tableName,
				target);
		// add date conditions
		//sql: STR_TO_DATE('2014-05-28 11:30:10','%Y-%m-%d');
		//[order=0, tableName=FIXED_ASSET_REGISTRATION, query=null, 
		//beforeDate=2015-02-20, 
		//afterDate=2014-11-21, 
		//lastUpdated=false, 
		//created=true]
		String dateConditionStmt = null;
		String stampName = null;
		if(lastUpdated){
			stampName = "LAST_UPDATED_STAMP";
		}else if(created){
			stampName = "CREATED_STAMP";
		}
		if(stampName!=null){
			String sqlStmBeforeDate = null;
			if (this.beforeDate != null && !this.beforeDate.isEmpty()) {
				
				sqlStmBeforeDate = ""+stampName+" < "+ "STR_TO_DATE('"+this.beforeDate+"','%Y-%m-%d')";
	
			}
			String sqlStmAfterDate = null;
			if (this.afterDate != null && !this.afterDate.isEmpty()) {
				sqlStmAfterDate = ""+stampName+" > "+ "STR_TO_DATE('"+this.afterDate+"','%Y-%m-%d')";
			}
			
			dateConditionStmt = (sqlStmBeforeDate!=null?sqlStmBeforeDate:"")+(sqlStmAfterDate!=null?(sqlStmBeforeDate!=null?" and ":"")+sqlStmAfterDate:"");
		}
		if(dateConditionStmt!=null){
			log.debug("buildTableQueryComponentsObj: dateConditionStmt: "+dateConditionStmt);
			tqc.setDateConditionStmt(dateConditionStmt);
		
		}
		return tqc;
	}

	public int getOrder() {
		return order;
	}

	public String getTableName() {
		return tableName;
	}

	public String getQuery() {
		return query;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getBeforeDate() {
		return beforeDate;
	}

	public void setBeforeDate(String beforeDate) {
		this.beforeDate = beforeDate;
	}

	public String getAfterDate() {
		return afterDate;
	}

	public void setAfterDate(String afterDate) {
		this.afterDate = afterDate;
	}

	public boolean isLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(boolean lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public boolean isCreated() {
		return created;
	}

	public void setCreated(boolean created) {
		this.created = created;
	}

	@Override
	public String toString() {
		return "TableQueryModel [order=" + order + ", tableName=" + tableName
				+ ", query=" + query + ", beforeDate=" + beforeDate
				+ ", afterDate=" + afterDate + ", lastUpdated=" + lastUpdated
				+ ", created=" + created + "]";
	}
}
