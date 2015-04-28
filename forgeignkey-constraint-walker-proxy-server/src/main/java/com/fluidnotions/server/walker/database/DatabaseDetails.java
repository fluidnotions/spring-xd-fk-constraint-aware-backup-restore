package com.fluidnotions.server.walker.database;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.sourceforge.schemaspy.model.Database;

import org.apache.commons.dbcp2.BasicDataSourceFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;

public class DatabaseDetails {
	
	private static final Log log = LogFactory.getLog(DatabaseDetails.class);
	
	private String exportModuleName = "jdbcexport";
	private String importModuleName = "jdbcimport";
	
	private String def;

	private Map<String, Integer> insertionOrderRefMap;
	
	private String url; 
	private String username; 
	private String password; 
	private String driver;
	
	//these fields are used as indicators 
	//if exportDir is null this assumed to be a import only data source & new import def or stream tap is created
	private String exportDir;
	//if importDir is null this assumed to be a export only data source & new export def or stream tap is created
	private String importDir;
	
	public boolean isExportOnly(){
		return importDir==null||importDir.isEmpty();
	}
	
	public boolean isImportOnly(){
		return exportDir==null||exportDir.isEmpty();
	}
	
	private String exportFileExtension;
	
	private Database metaData;
	private JdbcTemplate jdbcTemplate;

	
		
	public DatabaseDetails(String def, String url, String username, String password, String driver, String exportDir, String importDir, String exportFileExtension){
		this.def = def;
		this.url = url;
		this.username = username;
		this.password = password;
		this.driver = driver;
		
		this.importDir = importDir;
		this.exportDir = exportDir;
		this.exportFileExtension = exportFileExtension;
		
		
		init();
	}
	
	/*"definition":"jdbcxmlexport
		 --commitInterval=1000
		 --dbPassword=mysql
		 --dbUrl=jdbc:mysql://localhost:3306/opentaps
		 --dbUsername=root
		 --driverClassName=com.mysql.jdbc.Driver
		 --exportDir=/home/justin/just-dev/xd-dist/xd-customized-singlenode-setup/xd-out/${xd.job.name}
		 --exportFileExtension=xml
		 --restartable=true
		 --dateFormat=yyyy-MM-dd
		 --makeUnique=true"
	*/	
	public boolean checkXdExportJobDefinitionAgaints(String xdDefinition){
		log.debug("xdDefinition: "+xdDefinition);
		//xdDefinition: jdbcxmlexport --commitInterval=1000
		//get plain parameter string
		int start = this.exportModuleName.length();
		xdDefinition = xdDefinition.substring(start, xdDefinition.length()).trim();
		log.debug("xdDefinition(plain parameter substring): "+xdDefinition);
		
		boolean match = true;
		
		String[] props = xdDefinition.split("--");
		Map<String, String> xdDefPropMap = new HashMap<String, String>();
		for(String p: props){
			if (!p.isEmpty()) {
				String key = p.substring(0, p.indexOf("=")).trim();
				String value = p.substring(p.indexOf("=") + 1, p.length())
						.trim();
				log.debug("key: " + key + ", value: " + value);
				xdDefPropMap.put(key, value);
			}
		}
		if(!url.equals(xdDefPropMap.get("dbUrl"))){
			log.debug(url +" does not equal "+ xdDefPropMap.get("dbUrl"));
			match = false;
		}
		if(!username.equals(xdDefPropMap.get("dbUsername"))){
			log.debug( username+" does not equal "+ xdDefPropMap.get("dbUsername"));
			match = false;		
		}
		if(!password.equals(xdDefPropMap.get("dbPassword"))){
			log.debug( password+" does not equal "+ xdDefPropMap.get("dbPassword"));
			match = false;
		}
		if(!driver.equals(xdDefPropMap.get("driverClassName"))){
			log.debug( driver + " does not equal "+ xdDefPropMap.get("driverClassName"));
			match = false;
		}
		///home/justin/just-dev/xd-dist/xd-customized-singlenode-setup/xd-out/${xd.job.name}
		String xdExportDir = xdDefPropMap.get("exportDir");
		if(!exportDir.endsWith("/${xd.job.name}")){
			xdExportDir = xdExportDir.replace("/${xd.job.name}", "");
		}
		if(!exportDir.equals(xdExportDir)){
			log.debug( exportDir+" does not equal "+xdDefPropMap.get("exportDir") );
			match = false;		
		}
		if(!exportFileExtension.equals(xdDefPropMap.get("exportFileExtension"))){
			log.debug( exportFileExtension+" does not equal "+ xdDefPropMap.get("exportFileExtension"));
			match = false;
		}
	
		return match;
	}

	public boolean checkXdExportStreamDefinitionAgaints(String xdDefinition, String xdDatasourceUrl, String xdDatasourceUsername, String xdDatasourcePassword) {
		return checkXdStreamDefinitionAgaints(this.getDefExport(),  xdDefinition,  xdDatasourceUrl,  xdDatasourceUsername,  xdDatasourcePassword);
	}

	public boolean checkXdImportJobDefinitionAgaints(String xdDefinition) {
		log.debug("xdDefinition: "+xdDefinition);
		//xdDefinition: jdbcxmlexport --commitInterval=1000
		//get plain parameter string
		int start = this.importModuleName.length();
		xdDefinition = xdDefinition.substring(start, xdDefinition.length()).trim();
		log.debug("xdDefinition(plain parameter substring): "+xdDefinition);
		
		boolean match = true;
		
		String[] props = xdDefinition.split("--");
		Map<String, String> xdDefPropMap = new HashMap<String, String>();
		for(String p: props){
			if (!p.isEmpty()) {
				String key = p.substring(0, p.indexOf("=")).trim();
				String value = p.substring(p.indexOf("=") + 1, p.length())
						.trim();
				log.debug("key: " + key + ", value: " + value);
				xdDefPropMap.put(key, value);
			}
		}
		if(!url.equals(xdDefPropMap.get("dbUrl"))){
			log.debug(url +" does not equal "+ xdDefPropMap.get("dbUrl"));
			match = false;
		}
		if(!username.equals(xdDefPropMap.get("dbUsername"))){
			log.debug( username+" does not equal "+ xdDefPropMap.get("dbUsername"));
			match = false;		
		}
		if(!password.equals(xdDefPropMap.get("dbPassword"))){
			log.debug( password+" does not equal "+ xdDefPropMap.get("dbPassword"));
			match = false;
		}
		if(!driver.equals(xdDefPropMap.get("driverClassName"))){
			log.debug( driver + " does not equal "+ xdDefPropMap.get("driverClassName"));
			match = false;
		}
		///home/justin/just-dev/xd-dist/xd-customized-singlenode-setup/xd-out/${xd.job.name}
		String importDir = xdDefPropMap.get("importDir");
		if(!importDir.endsWith("/${xd.job.name}")){
			importDir = importDir.replace("/${xd.job.name}", "");
		}
		if(!exportDir.equals(importDir)){
			log.debug( importDir+" does not equal "+xdDefPropMap.get("importDir") );
			match = false;		
		}
		
		return match;
	}

	public boolean checkXdImportStreamDefinitionAgaints(String xdDefinition, String xdDatasourceUrl, String xdDatasourceUsername, String xdDatasourcePassword) {
		return checkXdStreamDefinitionAgaints(this.getDefImport(),  xdDefinition,  xdDatasourceUrl,  xdDatasourceUsername,  xdDatasourcePassword);
	}

	private boolean checkXdStreamDefinitionAgaints(String def, String xdDefinition, String xdDatasourceUrl, String xdDatasourceUsername, String xdDatasourcePassword) {
		//tap:job:xmlexport2.job > transform | jdbc 
		//--tableName=MY_XD_JOB_EXECUTION_STATUS_PAYLOAD 
		//--url=jdbc:mysql://localhost/xdjob 
		//--username=root 
		//--password=mysql 
		//--driverClassName=com.mysql.jdbc.Driver
		log.debug("stream: xdDefinition: "+xdDefinition);
		//get plain parameter string
		String prefix = "tap:job:"+def+".job > transform | jdbc ";
		int start = prefix.length();
		xdDefinition = xdDefinition.substring(start, xdDefinition.length());
		log.debug("stream: xdDefinition(plain parameter substring): "+xdDefinition);
		
		boolean match = true;
		
		String[] props = xdDefinition.split("--");
		Map<String, String> xdDefPropMap = new HashMap<String, String>();
		for(String p: props){
			if(!p.isEmpty()){
				String key = p.substring(0, p.indexOf("=")).trim();
				String value = p.substring(p.indexOf("=")+1, p.length()).trim();
				log.debug("key: "+key+", value: "+value);
				xdDefPropMap.put(key, value);
			}
		}
		if(!xdDatasourceUrl.equals(xdDefPropMap.get("url"))){
			match = false;
		}
		if(!xdDatasourceUsername.equals(xdDefPropMap.get("username"))){
			match = false;		
		}
		if(!xdDatasourcePassword.equals(xdDefPropMap.get("password"))){
			match = false;
		}
		
		return match;
	}

	public String createXdExportJobDefString(){
		String def =
				this.exportModuleName+
				 " --commitInterval=1000"+
				 " --dbPassword="+this.password+
				 " --dbUrl="+this.url+
				 " --dbUsername="+this.username+
				 " --driverClassName="+this.driver+
				 " --exportDir="+this.exportDir+"/${xd.job.name}"+
				 " --exportFileExtension="+this.exportFileExtension+
				 " --restartable=true"+
				 " --dateFormat=yyyy-MM-dd"+
				 " --makeUnique=true";
		
		return def;
	}

	public String createXdExportStreamDefString(String xdDatasourceUrl, String xdDatasourceUsername, String xdDatasourcePassword) {
		return createXdStreamDefString(this.getDefExport(),  xdDatasourceUrl,  xdDatasourceUsername,  xdDatasourcePassword);
	}


	public String createXdImportJobDefString(String uploadUrlDirectoryBase) {
		String def =
				this.importModuleName+
				 " --commitInterval=1000"+
				 " --dbPassword="+this.password+
				 " --dbUrl="+this.url+
				 " --dbUsername="+this.username+
				 " --driverClassName="+this.driver+
				 " --importDir="+this.importDir+"/${xd.job.name}"+
				 " --uploadDir="+uploadUrlDirectoryBase+
				 " --restartable=true"+
				 " --dateFormat=yyyy-MM-dd"+
				 " --makeUnique=true";
		
		return def;
	}
	

	//helper methods
	
		public String createXdImportStreamDefString(String xdDatasourceUrl, String xdDatasourceUsername, String xdDatasourcePassword) {
		return createXdStreamDefString(this.getDefImport(),  xdDatasourceUrl,  xdDatasourceUsername,  xdDatasourcePassword);
	}
	
	//tap:job:xmlexport1.job > transform | jdbc 
	//--tableName=MY_XD_JOB_EXECUTION_STATUS_PAYLOAD 
	//--url=jdbc:mysql://localhost/xdjob 
	//--username=root 
	//--password=mysql 
	//--driverClassName=com.mysql.jdbc.Driver
	private String createXdStreamDefString(String defname, String xdDatasourceUrl, String xdDatasourceUsername, String xdDatasourcePassword) {
		String def ="tap:job:"+defname+".job > transform | jdbc"+
					" --tableName=MY_XD_JOB_EXECUTION_STATUS_PAYLOAD"+
					" --url="+xdDatasourceUrl+
					" --username="+xdDatasourceUsername+
					" --password="+xdDatasourcePassword+
					" --driverClassName=com.mysql.jdbc.Driver";
		return def;
	}
	
	public String getDefExport() {
		return def+"export";
	}
	
	public String getDefImport() {
		return def+"import";
	}
	
	public Map<String, Integer> getInsertionOrderRefMap() {
		return insertionOrderRefMap;
	}
	
	
	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}
	
	public Database getMetaData() {
		return metaData;
	}
	
	private void init(){
		Properties prop = new Properties();
		
		prop.put("username", username);
		prop.put("password", password);
		prop.put("driverClassName", driver);
		prop.put("url", url);
		prop.put("removeAbandoned", false);
		prop.put("removeAbandonedTimeout", 300);
		prop.put("logAbandoned", true);
		prop.put("maxActive", 10);
		
		try {
			this.jdbcTemplate = new JdbcTemplate(BasicDataSourceFactory.createDataSource(prop));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setInsertionOrderRefMap(Map<String, Integer> insertionOrderRefMap) {
		this.insertionOrderRefMap = insertionOrderRefMap;
	}
	
	
	public void setMetaData(Database metaData) {
		this.metaData = metaData;
	}

}