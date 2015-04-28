package com.fluidnotions.springbatch.export;

import static org.springframework.xd.module.options.spi.ModulePlaceholders.XD_JOB_NAME;

import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Captures options for the {@code jdbcxmlexport} job.
 * 
 */
public class JdbcExportOptionsMetadata {

	// the commit interval to be used for the step
	private int commitInterval = 1000;
	// whether the job should be restartable or not in case of failure
	private boolean restartable = true;
	// database details
	private String driverClassName = "com.mysql.jdbc.Driver";
	// url including database name eg: sample
	private String dbUrl = "jdbc:mysql://localhost:3306/opentaps";
	// database username
	private String dbUsername = "root";
	// database password
	private String dbPassword = "mysql";
	// this is the root directory files are written/read from/to
	private String exportDir = "/home/justin/just-dev/xd-dist/xd-customized-singlenode-setup/xd-out/" + XD_JOB_NAME;
	// the extension of the export file
	private String exportFileExtension = "json";
	// database name - for RELATIONAL_DATA_PORT_JOB_PARAM lookups
	/*private String targetDatabaseName = "";


	@ModuleOption("database name - for RELATIONAL_DATA_PORT_JOB_PARAM lookups")
	public void setTargetDatabaseName(String targetDatabaseName) {
		this.targetDatabaseName = targetDatabaseName;
	}*/
	
	@ModuleOption("this is the root directory files are written/read from/to")
	public void setExportDir(String exportDir) {
		this.exportDir = exportDir;
	}

	@ModuleOption("the commit interval to be used for the step")
	public void setCommitInterval(int commitInterval) {
		this.commitInterval = commitInterval;
	}

	@ModuleOption("whether the job should be restartable or not in case of failure")
	public void setRestartable(boolean restartable) {
		this.restartable = restartable;
	}

	@ModuleOption(value = "database details")
	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	@ModuleOption(value = "url including database name eg: sample")
	public void setDbUrl(String dbUrl) {
		this.dbUrl = dbUrl;
	}

	@ModuleOption(value = "database username")
	public void setDbUsername(String dbUsername) {
		this.dbUsername = dbUsername;
	}

	@ModuleOption(value = "database password")
	public void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}

	@ModuleOption(value = "the extension of the export file")
	public void setExportFileExtension(String exportFileExtension) {
		this.exportFileExtension = exportFileExtension;
	}
	
	public String getExportDir() {
		return exportDir;
	}
	
	/*public String getTargetDatabaseName() {
		return targetDatabaseName;
	}*/

	public int getCommitInterval() {
		return commitInterval;
	}

	public boolean getRestartable() {
		return restartable;
	}

	public String getDriverClassName() {
		return driverClassName;
	}

	public String getDbUrl() {
		return dbUrl;
	}

	public String getDbUsername() {
		return dbUsername;
	}

	public String getDbPassword() {
		return dbPassword;
	}

	public String getExportFileExtension() {
		return exportFileExtension;
	}


}