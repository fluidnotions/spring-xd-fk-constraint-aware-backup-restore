package com.fluidnotions.springbatch.iimport.extended;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcParameterUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.util.Assert;
import org.springframework.xd.tuple.DefaultTuple;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.batch.TupleSqlParameterSourceProvider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TupleJdbcBatchItemWriter implements ItemWriter<Tuple>,
		InitializingBean {

	protected static final Log logger = LogFactory
			.getLog(TupleJdbcBatchItemWriter.class);
	/**
	 * When you use INSERT IGNORE, then the row won't actually be inserted
	 * if it results in a duplicate key. But the statement won't generate an
	 * error. It generates a warning instead. These cases include:
	 * 
	 * Inserting a duplicate key in columns with PRIMARY KEY or UNIQUE
	 * constraints.
	 * 
	 * Inserting a NULL into a column with a NOT NULL constraint.
	 * 
	 * Inserting a row to a partitioned table, but the values you insert
	 * don't map to a partition.
	 **/
	private boolean useInsertIgnore = true;

	private NamedParameterJdbcOperations namedParameterJdbcTemplate;

	private TupleSqlParameterSourceProvider itemSqlParameterSourceProvider;

	private DataSource moduleDataSource;
	private DataSource dataSource;

	private String jobDef;
	private String jobKey;

	private static ObjectMapper mapper = new ObjectMapper();

	private SimpleJdbcInsert simpleJdbcInsertForMyXdImportReport;

	public TupleJdbcBatchItemWriter() {
		this.itemSqlParameterSourceProvider = new TupleSqlParameterSourceProvider();
	}

	public void setModuleDataSource(DataSource dataSource) {
		this.moduleDataSource = dataSource;
	}

	public void setJdbcTemplate(
			NamedParameterJdbcOperations namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public void setJobKey(String jobKey) {
		this.jobKey = jobKey;
	}

	public void setJobDef(String jobDef) {
		this.jobDef = jobDef;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	private SimpleJdbcInsert getSimpleJdbcInsertForMyXdImportReport() {
		if (simpleJdbcInsertForMyXdImportReport == null) {
			simpleJdbcInsertForMyXdImportReport = new SimpleJdbcInsert(
					new JdbcTemplate(dataSource))
					.withTableName("MY_XD_IMPORT_REPORT");
		}
		return simpleJdbcInsertForMyXdImportReport;
	}

	/**
	 * Check mandatory properties - there must be a SimpleJdbcTemplate and an
	 * SQL statement plus a parameter source.
	 */
	@Override
	public void afterPropertiesSet() {
		Assert.notNull(moduleDataSource, "A Module DataSource is required.");
		Assert.notNull(dataSource, "A DataSource is required.");
		Assert.notNull(jobDef, "We need a jobDef to save the report");
		Assert.notNull(jobKey, "We need a jobKey to save the report");
		if (namedParameterJdbcTemplate == null) {
			this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(
					moduleDataSource);
		}
	}

	public void write(final List<? extends Tuple> items) throws Exception {

		if (!items.isEmpty()) {

			Map<String, List<SqlParameterSource>> tableParameterGroups = new LinkedHashMap<String, List<SqlParameterSource>>();
			Map<String, TableInsertReport> report = new HashMap<String, TableInsertReport>();

			// used to decide when to start a new list group
			String tableName = null;
			// used as actual key so it doesn't need to be rebuild every time
			String tableSql = null;
			List<SqlParameterSource> batchArgsList = null;
			logger.debug("Executing batch with " + items.size() + " items.");
			int itt = 1;
			for (Tuple item : items) {
				logger.debug(itt++
						+ ". Building SqlParameterSource for tableName: "
						+ item.getString("tableName"));

				if (tableName == null
						|| !tableName.equals(item.getString("tableName"))) {
					tableName = item.getString("tableName");

					batchArgsList = new ArrayList<SqlParameterSource>();
					tableSql = buildSqlFromItemBatchSample(item);
					tableParameterGroups.put(tableSql, batchArgsList);
				}

				tableParameterGroups.get(tableSql).add(
						itemSqlParameterSourceProvider
								.createSqlParameterSource(item));

				if (!report.containsKey(tableSql)) {
					report.put(tableSql, new TableInsertReport(/*tableSql,*/
							tableName));
				}

			}
			// second pass only for failed table inserts is handled via
			// recursion internally
			doBatchUpdateForTableGroup(tableParameterGroups, report, false);

			// process report
			logger.debug("--------------------------Report--------------------------");
			for (TableInsertReport r : report.values()) {
				logger.debug(r.toString());
			}
			logger.debug("----------------------------------------------------------");

			// store to report for either warning or errors
			for (TableInsertReport r : report.values()) {
				if (r.isError() || r.isWarning()) {
					Map<String, Object> importReportRecord = new HashMap<String, Object>();
					importReportRecord.put("jobKey", jobKey);
					importReportRecord.put("jobDef", jobDef);
					importReportRecord.put("statusType", r.isError() ? "ERROR"
							: (r.isWarning() ? "WARNING" : ""));
					importReportRecord.put("tableName", r.tableName);
					importReportRecord.put("data", r.toString());

					getSimpleJdbcInsertForMyXdImportReport().execute(
							importReportRecord);

				}
			}

		}
	}

	private void doBatchUpdateForTableGroup(
			Map<String, List<SqlParameterSource>> tableParameterGroups,
			Map<String, TableInsertReport> report, boolean isSecondPass) {

		Map<String, List<SqlParameterSource>> firstPassFailErrors = new LinkedHashMap<String, List<SqlParameterSource>>();

		int itt2 = 1;
		for (Map.Entry<String, List<SqlParameterSource>> tableParameterGroup : tableParameterGroups
				.entrySet()) {

			logger.debug(itt2++ + ". batchUpdate ("+(isSecondPass?"SECOND PASS":"FIRST PASS")+") for table query: "+ tableParameterGroup.getKey());
			String exceptionMsg = null;
			int[] updateCounts = null;
			try {
				updateCounts = namedParameterJdbcTemplate.batchUpdate(
						tableParameterGroup.getKey(),
						tableParameterGroup.getValue().toArray(
								new SqlParameterSource[tableParameterGroup
										.getValue().size()]));
			} catch (DataIntegrityViolationException e) {
				try {
					exceptionMsg = e.getMessage().substring(
							e.getMessage().indexOf(":") + 1,
							e.getMessage().lastIndexOf(";"));
				} catch (Exception e1) {
					exceptionMsg = e.getMessage();
				}
				// only keep track on first pass on second they go into the
				// report
				if (!isSecondPass) {
					firstPassFailErrors.put(tableParameterGroup.getKey(),
							tableParameterGroup.getValue());
				}

			}

			// add up update counts on table for report
			int c = 0;
			if (updateCounts != null && updateCounts.length > 0) {
				for (Integer i : updateCounts) {
					c += i;
				}
			}

			TableInsertReport tir = report.get(tableParameterGroup.getKey());

			if (!isSecondPass) {
				tir.firstPassRecordInputCount += tableParameterGroup.getValue()
						.size();
				tir.firstPassUpdateCountTotal += c;
				if (exceptionMsg != null)
					tir.firstPassError += exceptionMsg;
			} else {
				tir.didSecondPass = true;
				tir.secondPassRecordInputCount += tableParameterGroup
						.getValue().size();
				tir.secondPassUpdateCountTotal += c;
				if (exceptionMsg != null)
					tir.secondPassError += exceptionMsg;
			}

		}
		// do second pass if there are errors from first, we only track first
		// pass errors second pass errors are logged in the report
		if (firstPassFailErrors.size() > 0) {
			doBatchUpdateForTableGroup(firstPassFailErrors, report, true);
		}

	}

	private String buildSqlFromItemBatchSample(Tuple tuple) {
		List<String> names = ((DefaultTuple) tuple).getFieldNames();
		StringBuilder columns = new StringBuilder();
		StringBuilder namedParams = new StringBuilder();
		for (String column : names) {
			if (column.equals("tableName"))
				continue;
			if (columns.length() > 0) {
				columns.append(", ");
				namedParams.append(", ");
			}
			columns.append(column);
			namedParams.append(":").append(column.trim());
		}
		
		String sql = "insert"+(useInsertIgnore?" ignore ":" ")+"into " + tuple.getString("tableName")
				+ " (" + columns.toString() + ") values ("
				+ namedParams.toString() + ")";

		List<String> namedParameters = new ArrayList<String>();
		int parameterCount = JdbcParameterUtils.countParameterPlaceholders(sql,
				namedParameters);
		if (namedParameters.size() > 0) {
			if (parameterCount != namedParameters.size()) {
				throw new InvalidDataAccessApiUsageException(sql);
			}

		}
		return sql;
	}

	 class TableInsertReport {
		
		String tableName;
		boolean didSecondPass;
		String firstPassError;
		String secondPassError;
		Integer secondPassRecordInputCount;
		Integer secondPassUpdateCountTotal;
		Integer firstPassRecordInputCount;
		Integer firstPassUpdateCountTotal;
		
		
		//String sql;

		public TableInsertReport(/*String sql, */String tableName) {
			super();
			//this.sql = sql;
			this.tableName = tableName;
			this.firstPassRecordInputCount = 0;
			this.firstPassUpdateCountTotal = 0;
			this.secondPassRecordInputCount = 0;
			this.secondPassUpdateCountTotal = 0;
			this.firstPassError = "";
			this.secondPassError = "";
			this.didSecondPass = false;
		}

		public boolean isWarning() {
			return (secondPassRecordInputCount != secondPassUpdateCountTotal) || (firstPassRecordInputCount != firstPassUpdateCountTotal);
		}

		public boolean isError() {
			return !secondPassError.isEmpty();
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

		public Integer getFirstPassRecordInputCount() {
			return firstPassRecordInputCount;
		}

		public Integer getFirstPassUpdateCountTotal() {
			return firstPassUpdateCountTotal;
		}

		public Integer getSecondPassRecordInputCount() {
			return secondPassRecordInputCount;
		}

		public Integer getSecondPassUpdateCountTotal() {
			return secondPassUpdateCountTotal;
		}

		/*public String getSql() {
			return sql;
		}*/

		@Override
		public String toString() {
			String json = "TableInsertReport [tableName="
					+ tableName + ", firstPassRecordInputCount="
					+ firstPassRecordInputCount
					+ ", firstPassUpdateCountTotal="
					+ firstPassUpdateCountTotal + ", didSecondPass="
					+ didSecondPass + ", secondPassRecordInputCount="
					+ secondPassRecordInputCount
					+ ", secondPassUpdateCountTotal="
					+ secondPassUpdateCountTotal + ", firstPassError="
					+ firstPassError + ", secondPassError=" + secondPassError
					+ "]";
			try {
				json = mapper.writeValueAsString(this);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			return json;
		}

	}

}
