package com.fluidnotions.server.walker.database;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.xd.tuple.Tuple;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fluidnotions.server.walker.model.TableInsertReport;

@Component
public class CheckXdJobStatus {

	private static final Log log = LogFactory.getLog(CheckXdJobStatus.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Value("${xdjobcompletetimeout}")
	private String xdjobcompletetimeout;
	
	private static ObjectMapper mapper = new ObjectMapper();

	public String isJobComplete(String jobKey) {

		String result = null;
		int pollingTimeout = 0;
		boolean recordsFound = false;
		try {
			outer: while (true) {

				List<Tuple> rs = jdbcTemplate.query(
						"SELECT * FROM MY_XD_JOB_EXECUTION_STATUS_PAYLOAD where payload like '%jobKey="
								+ jobKey.trim() + "%'", new TupleRowMapper());
				for (Tuple t : rs) {
					recordsFound = true;
					
					if (t.getString("payload").contains("status=COMPLETED")) {
						log.debug("isJobComplete: payload: "
								+ t.getString("payload"));
						result = "COMPLETED";
						break outer;
					} else if (t.getString("payload").contains("status=FAILED")) {
						String payload = t.getString("payload");
						int keylen = "exitDescription=".length();
						int start = payload.indexOf("exitDescription=")
								+ keylen;
						String ex = null;
						if(payload.indexOf("\n", start)>-1){
							 ex = payload.substring(start,
									payload.indexOf("\n", start));
							
						}
						result = "FAILED"+(ex!=null?" with " + ex:"");
						log.error("jobKey: " + jobKey
								+ " failed on xd with error msg: " + (ex!=null?" with " + ex:" could not extract error msg"));
						break outer;
					}
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					log.error(
							"problem with isJobComplete jdbc polling loop thread sleep",
							e);
					break outer;
				}
				
				if(pollingTimeout == new Integer(xdjobcompletetimeout.trim())){
					result = "FAILED with (timeout is set to:"+xdjobcompletetimeout+" sec) "+(recordsFound? "unknown":"no records with jobKey ("+jobKey+") found in datasource before timeout");
					log.error("jobKey: " + jobKey
							+ " failed with error msg: " + (recordsFound? "unknown":"no records with jobKey ("+jobKey+") found in datasource"));
					break outer;
				}
				pollingTimeout++;
			}
		} catch (Throwable t) {
			t.printStackTrace();
			log.error("problem with isJobComplete jdbc polling loop", t);
		}

		return result;
	}
	
	public List<TableInsertReport> getImportJobReport(String jobKey, String importJobDef) {

		List<TableInsertReport> report = new ArrayList<TableInsertReport>();
		boolean recordsFound = false;
		try {
			 
				String sql = "SELECT * FROM MY_XD_IMPORT_REPORT where jobDef = '"+importJobDef+"' and jobKey = '"+jobKey+"'";
				log.debug("sql: "+sql);
				List<Tuple> rs = jdbcTemplate.query(
						sql, new TupleRowMapper());
				for (Tuple t : rs) {
					recordsFound = true;
					
					report.add(mapper.readValue((t.getString("data")), TableInsertReport.class));
				}
				log.debug("recordsFound: "+recordsFound+", report contains "+report.size()+" items");

		} catch (Throwable t) {
			t.printStackTrace();
			log.error("problem with getImportJobReport jdbc lookup", t);
		}

		return recordsFound?report:null;
	}

}
