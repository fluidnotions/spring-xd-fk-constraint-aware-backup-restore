package com.fluidnotions.server.controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fluidnotions.server.walker.DeleteWalker;
import com.fluidnotions.server.walker.SelectWalker;
import com.fluidnotions.server.walker.WalkerUtil;
import com.fluidnotions.server.walker.database.CheckXdJobStatus;
import com.fluidnotions.server.walker.database.DatabasesMetaDataMap;
import com.fluidnotions.server.walker.database.RelationalDependencyDelete;
import com.fluidnotions.server.walker.model.TableInsertReport;
import com.fluidnotions.server.walker.model.TableQueryComponents;
import com.fluidnotions.server.walker.model.TableQueryModel;
import com.fluidnotions.server.walker.model.UploadedFile;

@RestController
public class Controller {

	private static final Log log = LogFactory.getLog(Controller.class);

	@Autowired
	private DeleteWalker deleteWalker;

	@Autowired
	private SelectWalker selectWalker;

	@Autowired
	private WalkerUtil walkerUtil;

	@Autowired
	private RelationalDependencyDelete relationDependencyDelete;
	
	@Autowired
	private CheckXdJobStatus checkXdJobStatus;
	
	@Autowired
	private DatabasesMetaDataMap databasesMetaDataMap;

	@RequestMapping(value = "/tables/{xdJobDefName}", method = RequestMethod.POST)
	public List<TableQueryModel> retrieveTableNameList(@PathVariable String xdJobDefName) {

		List<TableQueryModel> tableNames = null;
		try {
			tableNames = selectWalker.retrieveCompleteTableNameList(xdJobDefName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return tableNames;
	}

	@RequestMapping(value = "/selectquerylistfor/{xdJobDefName}", method = RequestMethod.POST)
	public List<TableQueryModel> fkCnstSelectQueryListFor(
			@RequestBody List<TableQueryModel> tableNames, @PathVariable String xdJobDefName) {
		if (log.isDebugEnabled()) {
			// set incoming as targets
			for (TableQueryModel tqm : tableNames) {
				log.debug(tqm.toString());
			}
		}
		
		//handle selection groups
		List<TableQueryComponents> all = selectWalker.run(xdJobDefName, tableNames);
		List<TableQueryComponents> consolidated = selectWalker.consolidateTablesQueries(all);
		List<TableQueryModel> tqms = new ArrayList<TableQueryModel>();

		if (log.isDebugEnabled()) {
			log.debug("---------------------------------all-----------------------------------------");
			for (TableQueryComponents q : all) {
				log.debug(q.toString() + "|toSqlQueryString : " + q.toSqlQueryString());
			}
			// debug - just makes it easier to see what's what, not important at
			// this point
			Collections.sort(consolidated,
					new Comparator<TableQueryComponents>() {
						public int compare(TableQueryComponents o1,
								TableQueryComponents o2) {
							return o1.getOrder() - o2.getOrder();
						}
					});
			log.debug("---------------------------------consolidated-----------------------------------------");

		}

		for (TableQueryComponents q : consolidated) {
			if (q != null) {
				TableQueryModel tqm = q.buildTableQueryModel();
				log.debug(tqm.getOrder() + ": " + tqm.getQuery());
				tqms.add(tqm);
			} else
				log.warn("Null element found in consolidated list.");
		}
		return tqms;
	}

	@RequestMapping(value = "/deletequerylistfor/{xdJobDefName}", method = RequestMethod.POST)
	public List<TableQueryModel> fkCnstDeleteQueryListFor(
			@RequestBody List<TableQueryModel> tableNames, @PathVariable String xdJobDefName) {

		// set incoming as targets
		if (log.isDebugEnabled()) {
			for (TableQueryModel tqm : tableNames) {
				log.debug(tqm.toString());
			}
		}

		// we only allow one target select at this point
		List<TableQueryComponents> all = deleteWalker.run(xdJobDefName, tableNames.get(0)
				.buildTableQueryComponentsObj(true));
		// we won't even try consolidate delete statements with composite fks -
		// we just order them, that's enough
		List<TableQueryModel> tqms = new ArrayList<TableQueryModel>();
		for (TableQueryComponents q : all) {
			TableQueryModel tqm = q.buildTableQueryModel();
			tqms.add(tqm);
		}
		walkerUtil.reorderSelectTableQueryList(tqms, databasesMetaDataMap.databaseMetaData(xdJobDefName));
		/*if (log.isDebugEnabled()) {
			for (TableQueryModel tqm : tqms) {
				log.debug(tqm.getOrder() + " -> " + tqm.getQuery());
			}
		}*/

		return tqms;
	}

	@RequestMapping(value = "/rundelseq/{xdJobDefName}", method = RequestMethod.POST)
	public void runDeleteSequence(@RequestBody List<TableQueryModel> tableNames, @PathVariable String xdJobDefName) {
		relationDependencyDelete.startSequentialDelete(xdJobDefName, tableNames);
	}

	@Value("${xdserverurl}")
	private String xdserverurl;
	
	
	@Value("${downloadUrlBase}")
	private String downloadUrlBase;


	@RequestMapping(value = "/backup/{xdJobDefName}", method = RequestMethod.POST)
	public SimpleResponse export(@RequestBody List<TableQueryModel> tableNames, @PathVariable String xdJobDefName) {
		String exportDef = databasesMetaDataMap.databaseMetaData(xdJobDefName).getDefExport();
		// debug
		log.debug("---------------------------------export/delete-----------------------------------------");
		String jobKey = walkerUtil
				.tableQueryModelListToRelationalPortJobEntity(exportDef, tableNames, databasesMetaDataMap.databaseMetaData(xdJobDefName));
		log.debug("export RelationalPortJobParam group job key: " + jobKey);
		// start job on spring xd
		RestTemplate restTemplate = new RestTemplate();
		String end = xdserverurl + "/jobs/executions";
		String jobParameters = "{\"-jobKey(string)\":\"" + jobKey+ "\"}";
		log.debug("postToXD: with params: exportDef: "+exportDef+", jobParameters: "+jobParameters);
		MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<String, String>();
		paramMap.add("jobname", exportDef);
		paramMap.add("jobParameters", jobParameters);
		boolean postToXDFailed = false;
		try {
			// no response expected from post to xd jobs/executions
			restTemplate.postForObject(end, paramMap, Object.class);
		} catch (Exception e) {
			log.error("problem with POST to "+end, e);
			postToXDFailed = true;
		}
		//poll database waiting for xd job to complete
		String result = null;
		if(postToXDFailed){
			result = "FAILED with unable to send post to xd";
		}else{
			result = checkXdJobStatus.isJobComplete(jobKey);
		}
		
		return new SimpleResponse(jobKey, exportDef, result, downloadUrlBase);
	}
	
	
	//for the import definition name we are just using export def + suffix "import" to reduce required refactoring
	@RequestMapping(value = "/restore/{xdJobDefName}", method = RequestMethod.POST)
	public SimpleResponse iimport(@RequestBody UploadedFile importJobParams, @PathVariable String xdJobDefName) {
		String importDef =  databasesMetaDataMap.databaseMetaData(xdJobDefName).getDefImport();
		// start job on spring xd
		RestTemplate restTemplate = new RestTemplate();
		String end = xdserverurl + "/jobs/executions";
		String jobParameters = "{\"-jobKey(string)\":\"" + importJobParams.getJobKey()+ "\", \"-importZipFileName(string)\":\"" + importJobParams.getName()+ "\"}";
		log.debug("postToXD: with params: importDef: "+importDef+", jobParameters: "+jobParameters);
		
		MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<String, String>();
		paramMap.add("jobname", importDef);
		paramMap.add("jobParameters", jobParameters);
		
		boolean postToXDFailed = false;
		try {
			// no response expected from post to xd jobs/executions
			restTemplate.postForObject(end, paramMap, Object.class);
		} catch (Exception e) {
			log.error("problem with POST to "+end, e);
			postToXDFailed = true;
		}
		//poll database waiting for xd job to complete
		String result = null;
		if(postToXDFailed){
			result = "FAILED with unable to send post to xd";
		}else{
			result = checkXdJobStatus.isJobComplete(importJobParams.getJobKey());
		}
		
		return new SimpleResponse(importJobParams.getJobKey(), importDef, result, null);
	}
	
	 @RequestMapping(value = "/restorejobreport/{xdJobDefName}", method = RequestMethod.GET)
	 public List<TableInsertReport> importJobReport(@RequestParam(value="jobKey", required=true) String jobKey, @PathVariable String xdJobDefName){
		 String importDef =  databasesMetaDataMap.databaseMetaData(xdJobDefName).getDefImport();
		 return checkXdJobStatus.getImportJobReport(jobKey, importDef);
	 }
	
	

	
	class SimpleResponse {
		private String jobKey;
		private String xdJobDefName;
		private String result;
		private String downloadUrlBase;
		public SimpleResponse(String jobKey, String xdJobDefName,
				String result, String downloadUrlBase) {
			super();
			this.jobKey = jobKey;
			this.xdJobDefName = xdJobDefName;
			this.result = result;
			this.downloadUrlBase = downloadUrlBase;
		}
		public String getJobKey() {
			return jobKey;
		}
		public String getXdJobDefName() {
			return xdJobDefName;
		}
		public String getResult() {
			return result;
		}
		public String getDownloadUrlBase() {
			return downloadUrlBase;
		}
		public void setJobKey(String jobKey) {
			this.jobKey = jobKey;
		}
		public void setXdJobDefName(String xdJobDefName) {
			this.xdJobDefName = xdJobDefName;
		}
		public void setResult(String result) {
			this.result = result;
		}
		public void setDownloadUrlBase(String downloadUrlBase) {
			this.downloadUrlBase = downloadUrlBase;
		}
		
		
		

		
	}

}
