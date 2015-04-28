package com.fluidnotions.springbatch.export.extended;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.xd.tuple.Tuple;

import com.fluidnotions.springbatch.TupleRowMapper;

public class QueryListPartitioner implements Partitioner {

	private DataSource dataSource;
	private String jobKey;
	private String targetDatabaseName;
	
	public void setTargetDatabaseName(String targetDatabaseName) {
		this.targetDatabaseName = targetDatabaseName;
	}

	public void setJobKey(String jobKey) {
		this.jobKey = jobKey;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	 /**   The sql queries need to be parsed into the batch/xd job and they are too long
	   *   for JobParamaters so we store them in the database we are working on, since it's 
	   *   available - it might be cleaner to store them in a new table in the batch database
	   *   with other batch related data. To do that you'd just set that datasource in the context &
	   *   of course just create the table in the 'batch' database
	   *   
	   *   Once stored the batch/xd export job uses the JOB_KEY which is parsed into JobParamaters
	   *   to look up the XD_RELATIONAL_DATA_PORT_JOB_PARAM group for the job. The QueryListPartitioner
	   *   then uses this group of XD_RELATIONAL_DATA_PORT_JOB_PARAM to set up the partitions to create a
	   *   file per table name, file name is prefixed with 'order' 
	   * 
	      CREATE TABLE `MY_XD_RELATIONAL_DATA_PORT_JOB_PARAM` (
		  `GUID` int(11) NOT NULL AUTO_INCREMENT,
		  `JOB_KEY` varchar(45) NOT NULL,
		  `FILE_NAME_PREFIX` varchar(45) NOT NULL,
		  `FILE_NAME` varchar(45) NOT NULL,
		  `SQL_QUERY` longtext NOT NULL,
		  `EXECUTED` int(1) DEFAULT NULL,
		  `JOB_DEF_NAME` varchar(45),
		  PRIMARY KEY (`GUID`)
		) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=latin1
	   **/
	private List<TableQuery> parseQueryList(){
		List<TableQuery> tqs = new ArrayList<TableQuery>();
		
		JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSource);
		List<Tuple> resultSet = jdbcTemplate.query("select * from MY_XD_RELATIONAL_DATA_PORT_JOB_PARAM where JOB_KEY = '"+this.jobKey.trim()+"'", new TupleRowMapper());
		for(Tuple t: resultSet){
			TableQuery tq = new TableQuery(this.jobKey, t.getString("FILE_NAME_PREFIX"), t.getString("FILE_NAME"), t.getString("SQL_QUERY") );
			tqs.add(tq);
		}
		
		return tqs;
	}

	/**
	 * @see Partitioner#partition(int)
	 * 
	 * In our cause gridSize is irrelevant
	 * since out gridSize is determined by the
	 * length of the query list parsed in with
	 * job parameters
	 * 
	 */
	public Map<String, ExecutionContext> partition(int gridSize) {

		Map<String, ExecutionContext> result = new HashMap<String, ExecutionContext>();
		int i = 0;
		for(TableQuery tq: parseQueryList()){
			ExecutionContext value = new ExecutionContext();
			result.put("partition" + (i++), value);
			value.putString("orderFileNamePrefix", tq.order);
			value.putString("tableFileName", tq.tableName);
			value.putString("sqlQuery", tq.sql);
			
		}

		return result;
	}
	
	class TableQuery{
		 String jobKey;
		 String order;
		 String tableName;
		 String sql;
		 
		public TableQuery(String jobKey, String order, String tableName, String sql) {
			super();
			this.jobKey = jobKey;
			this.order = order;
			this.tableName = tableName;
			this.sql = sql;
		}
		 	
	}
}