package com.fluidnotions.springbatch.iimport;

import static org.springframework.xd.module.options.spi.ModulePlaceholders.XD_JOB_NAME;

import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Captures options for the {@code jdbcxmlexport} job.
 * 
 */
public class JdbcImportOptionsMetadata {

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
		//this is the base folder where temp folders for each job archive are unzipped is already suffixed by ${xd.job.name} in com.fluidnotions.server.walker.database.DatabaseDetails.createXdImportJobDefString(String)
		private String importDir = "";
		//this is where the archives for the job are uploaded to by the proxy server common to all jobs
		private String uploadDir = "";
		
		// database name - for RELATIONAL_DATA_PORT_JOB_PARAM lookups
		/*private String targetDatabaseName = "";


		@ModuleOption("database name - for RELATIONAL_DATA_PORT_JOB_PARAM lookups")
		public void setTargetDatabaseName(String targetDatabaseName) {
			this.targetDatabaseName = targetDatabaseName;
		}*/
		
		@ModuleOption("the commit interval to be used for the step")
		public void setCommitInterval(int commitInterval) {
			this.commitInterval = commitInterval;
		}

		@ModuleOption("this is the base folder where temp folders for each job archive are unzipped is already suffixed by ${xd.job.name} in com.fluidnotions.server.walker.database.DatabaseDetails.createXdImportJobDefString(String)")
		public void setImportDir(String importDir) {
			this.importDir = importDir;
		}

		@ModuleOption("this is where the archives for the job are uploaded to by the proxy server")
		public void setUploadDir(String uploadDir) {
			this.uploadDir = uploadDir;
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
		
		public String getImportDir() {
			return importDir;
		}

		public String getUploadDir() {
			return uploadDir;
		}


		

}