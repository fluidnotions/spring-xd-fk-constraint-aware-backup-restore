package com.fluidnotions.server.walker.model;

public class TableInsertReport {
	
	private String tableName;
	private boolean didSecondPass;
	private String firstPassError;
	private String secondPassError;
	private Integer secondPassRecordInputCount;
	private Integer secondPassUpdateCountTotal;
	private Integer firstPassRecordInputCount;
	private Integer firstPassUpdateCountTotal;
	private boolean warning;
	private boolean error;
	
	public TableInsertReport(){
		
	}
	
	public String getTableName() {
		return tableName;
	}
	public boolean isDidSecondPass() {
		return didSecondPass;
	}
	public String getFirstPassError() {
		return firstPassError;
	}
	public String getSecondPassError() {
		return secondPassError;
	}
	public Integer getSecondPassRecordInputCount() {
		return secondPassRecordInputCount;
	}
	public Integer getSecondPassUpdateCountTotal() {
		return secondPassUpdateCountTotal;
	}
	public Integer getFirstPassRecordInputCount() {
		return firstPassRecordInputCount;
	}
	public Integer getFirstPassUpdateCountTotal() {
		return firstPassUpdateCountTotal;
	}
	public boolean isWarning() {
		return warning;
	}
	public boolean isError() {
		return error;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public void setDidSecondPass(boolean didSecondPass) {
		this.didSecondPass = didSecondPass;
	}
	public void setFirstPassError(String firstPassError) {
		this.firstPassError = firstPassError;
	}
	public void setSecondPassError(String secondPassError) {
		this.secondPassError = secondPassError;
	}
	public void setSecondPassRecordInputCount(Integer secondPassRecordInputCount) {
		this.secondPassRecordInputCount = secondPassRecordInputCount;
	}
	public void setSecondPassUpdateCountTotal(Integer secondPassUpdateCountTotal) {
		this.secondPassUpdateCountTotal = secondPassUpdateCountTotal;
	}
	public void setFirstPassRecordInputCount(Integer firstPassRecordInputCount) {
		this.firstPassRecordInputCount = firstPassRecordInputCount;
	}
	public void setFirstPassUpdateCountTotal(Integer firstPassUpdateCountTotal) {
		this.firstPassUpdateCountTotal = firstPassUpdateCountTotal;
	}
	public void setWarning(boolean warning) {
		this.warning = warning;
	}
	public void setError(boolean error) {
		this.error = error;
	}
	
	

}
