package com.fluidnotions.server.walker.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.sourceforge.schemaspy.Config;
import net.sourceforge.schemaspy.TableOrderer;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.ForeignKeyConstraint;
import net.sourceforge.schemaspy.model.InvalidConfigurationException;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.model.xml.SchemaMeta;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SchemaAnalyzer {

	private static final Log log = LogFactory.getLog(SchemaAnalyzer.class);
	private Config config;
	private static Database commonDatabaseMetadata;
	private static Map<String, Integer> commonInsertionOrderRefMap;
	
	@Value("${useSameMetaAndInsertOrderForAll}")
	private String useSameMetaAndInsertOrderForAll;

	public SchemaAnalyzer() {
		config = new Config();
		config.setOutputDir("/dummy/path/required");
		config.setHtmlGenerationEnabled(false);

	}

	public void analyze(DatabaseDetails dd) throws Exception {
		
		if(useSameMetaAndInsertOrderForAll.equalsIgnoreCase("true")){
			if(commonDatabaseMetadata == null){
				commonDatabaseMetadata = buildDatabaseMetadataObject(dd);
			}
			dd.setMetaData(commonDatabaseMetadata);
			if(commonInsertionOrderRefMap == null){
				Database databaseMetadataForInsertionOrderDiscovery = buildDatabaseMetadataObject(dd);
				commonInsertionOrderRefMap = setupInsertionOrderRefMap(getInsertionOrderedTables(databaseMetadataForInsertionOrderDiscovery));
				databaseMetadataForInsertionOrderDiscovery = null; //insure gc
			}
			dd.setInsertionOrderRefMap(commonInsertionOrderRefMap);
		}else{
			dd.setMetaData(buildDatabaseMetadataObject(dd));
			//Unfortunately the process of discovering insertion order alters the DatabaseMetadata Object in ways that break
			//the delete walker - so we need to create a new one - this may take too long when dealing with a high volume of
			//tenant data sources - hence the option to just use the same for all datasources
			Database databaseMetadataForInsertionOrderDiscovery = buildDatabaseMetadataObject(dd);
			dd.setInsertionOrderRefMap(setupInsertionOrderRefMap(getInsertionOrderedTables(databaseMetadataForInsertionOrderDiscovery)));
			databaseMetadataForInsertionOrderDiscovery = null; //insure gc
		}
					
	}

	private Database buildDatabaseMetadataObject(DatabaseDetails databaseDetails)
			throws SQLException {
		Database databaseMetadata = null;
		try {

			log.info("Starting schema analysis");

			Connection connection = databaseDetails.getJdbcTemplate().getDataSource()
					.getConnection();

			DatabaseMetaData meta = connection.getMetaData();
			String dbName = config.getDb();
			String schema = config.getSchema();

			if (schema == null && meta.supportsSchemasInTableDefinitions()
					&& !config.isSchemaDisabled()) {
				schema = config.getUser();
				if (schema == null)
					throw new InvalidConfigurationException(
							"Either a schema ('-s') or a user ('-u') must be specified");
				config.setSchema(schema);
			}

			SchemaMeta schemaMeta = config.getMeta() == null ? null
					: new SchemaMeta(config.getMeta(), dbName, schema);

			//
			// create our representation of the database
			//
			databaseMetadata = new Database(config, connection, meta, dbName,
					schema, new Properties(), schemaMeta);

			schemaMeta = null; // done with it so let GC reclaim it

		} catch (Config.MissingRequiredParameterException e) {
			log.fatal(
					"schema analyser failed to create database metadata object",
					e);
		}
		return databaseMetadata;
	}

	// the deletion order is the reverse Collections.reverse(orderedTables);
	private List<String> getInsertionOrderedTables(Database databaseMetadata) {
		List<String> insertionOrder = new ArrayList<String>();
		List<ForeignKeyConstraint> recursiveConstraints = new ArrayList<ForeignKeyConstraint>();
		// create an orderer to be able to determine insertion and
		// deletion ordering of tables
		TableOrderer orderer = new TableOrderer();
	
		// side effect is that the RI relationships get trashed
		// also populates the recursiveConstraints collection
		List<Table> orderedTables = orderer.getTablesOrderedByRI(databaseMetadata.getTables(),
				recursiveConstraints);
		for (Table t : orderedTables) {
			insertionOrder.add(t.getName());
		}
		orderedTables = null;

		return insertionOrder;
	}

	private Map<String, Integer> setupInsertionOrderRefMap(
			List<String> insertionOrder) {
		HashMap<String, Integer> insertionOrderRefMap = new HashMap<String, Integer>();

		Integer i = 0;
		for (String tableName : insertionOrder) {
			insertionOrderRefMap.put(tableName.trim(), i++);
		}

		return insertionOrderRefMap;
	}

	

}
