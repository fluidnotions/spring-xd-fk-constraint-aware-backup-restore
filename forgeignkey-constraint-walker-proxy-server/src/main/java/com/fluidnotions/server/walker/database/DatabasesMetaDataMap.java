package com.fluidnotions.server.walker.database;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.xd.tuple.Tuple;

import com.fluidnotions.server.walker.XdDefinitionSetup;

@Service
@Scope("singleton")
public class DatabasesMetaDataMap {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private XdDefinitionSetup xdDefinitionSetup;

	private SchemaAnalyzer schemaAnalyzer;
	private static Map<String, DatabaseDetails> databaseMetaDataMap;

	static {
		databaseMetaDataMap = new HashMap<>();
	}

	@Autowired
	public DatabasesMetaDataMap(SchemaAnalyzer schemaAnalyzer) {
		super();
		this.schemaAnalyzer = schemaAnalyzer;

	}

	@PostConstruct
	private void initStaticDatasourceMap() {
		List<Tuple> rs = jdbcTemplate.query("select * from MY_XD_JOB_MOD_DS",
				new TupleRowMapper());
		if (rs.size() > 0) {

			for (Tuple t : rs) {
				DatabaseDetails dd = new DatabaseDetails(
						t.getString("JOB_DEF_NAME"), t.getString("URL"),
						t.getString("USERNAME"), t.getString("PASSWORD"),
						t.getString("DRIVER"),
						t.getString("EXPORT_DIR"),
						t.getString("IMPORT_DIR"),
						t.getString("EXPORT_FILE_EXTENSION"));
				try {
					schemaAnalyzer.analyze(dd);
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (dd.getMetaData() != null) {
					databaseMetaDataMap.put(t.getString("JOB_DEF_NAME"), dd);
				}
			}

			xdDefinitionSetup.checkAllXdDefSetup(databaseMetaDataMap);

		}
	}

	public DatabaseDetails databaseMetaData(String def) {
		DatabaseDetails target = null;
		if (!databaseMetaDataMap.containsKey(def)) {

			List<Tuple> rs = jdbcTemplate.query(
					"select * from MY_XD_JOB_MOD_DS where JOB_DEF_NAME = "
							+ "\"" + def + "\"", new TupleRowMapper());
			if (rs.size() > 0) {
				Tuple t = rs.get(0);
				DatabaseDetails dd = new DatabaseDetails(
						t.getString("JOB_DEF_NAME"), t.getString("URL"),
						t.getString("USERNAME"), t.getString("PASSWORD"),
						t.getString("DRIVER"),
						t.getString("EXPORT_DIR"),
						t.getString("IMPORT_DIR"),
						t.getString("EXPORT_FILE_EXTENSION"));

				try {
					schemaAnalyzer.analyze(dd);
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (dd.getMetaData() != null) {
					databaseMetaDataMap.put(def, dd);
				}

			}
		}

		target = databaseMetaDataMap.get(def);

		return target;
	}

}
