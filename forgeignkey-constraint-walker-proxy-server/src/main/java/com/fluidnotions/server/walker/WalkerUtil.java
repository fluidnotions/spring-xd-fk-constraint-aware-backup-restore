package com.fluidnotions.server.walker;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;

import com.fluidnotions.server.walker.database.DatabaseDetails;
import com.fluidnotions.server.walker.model.TableQueryModel;

@Component
public class WalkerUtil {

	private static final Log log = LogFactory.getLog(WalkerUtil.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;



	/**
	 * The sql queries need to be parsed into the batch/xd job and they are too
	 * long for JobParamaters so we store them in the database we are working
	 * on, since it's available - it might be cleaner to store them in a new
	 * table in the batch database with other batch related data.
	 * 
	 * Once stored the batch/xd export job uses the JOB_KEY which is parsed into
	 * JobParamaters to look up the XD_RELATIONAL_DATA_PORT_JOB_PARAM group for
	 * the job. The QueryListPartitioner then uses this group of
	 * XD_RELATIONAL_DATA_PORT_JOB_PARAM to set up the partitions to create a
	 * file per table name, file name is prefixed with 'order'
	 * 
	 * CREATE TABLE `MY_XD_RELATIONAL_DATA_PORT_JOB_PARAM` ( `GUID` int(11) NOT
	 * NULL AUTO_INCREMENT, `JOB_KEY` varchar(45) NOT NULL, `FILE_NAME_PREFIX`
	 * varchar(45) NOT NULL, `FILE_NAME` varchar(45) NOT NULL, `SQL_QUERY`
	 * longtext NOT NULL, `EXECUTED` int(1) DEFAULT NULL, `JOB_DEF_NAME`
	 * varchar(45), PRIMARY KEY (`GUID`) ) ENGINE=InnoDB AUTO_INCREMENT=38
	 * DEFAULT CHARSET=latin1
	 **/
	public String tableQueryModelListToRelationalPortJobEntity(
			String xdJobDefName, List<TableQueryModel> tableNames, DatabaseDetails dd) {

		reorderSelectTableQueryList(tableNames, dd);

		// this identifies the group of parms for this job
		String jobKey = RandomStringUtils.randomAlphanumeric(5);

		// trying to use hibernate causes escape related issues with storing an
		// sql statement as a value
		SimpleJdbcInsert relationalDataPortJobParam = new SimpleJdbcInsert(
				jdbcTemplate)
				.withTableName("MY_XD_RELATIONAL_DATA_PORT_JOB_PARAM");
		for (TableQueryModel tqm : tableNames) {

			Map<String, String> insertMap = tqm
					.toRelationalDataPortJobParamMap();
			insertMap.put("JOB_KEY", jobKey);
			insertMap.put("JOB_DEF_NAME", xdJobDefName);

			try {
				relationalDataPortJobParam.execute(insertMap);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return jobKey;
	}
	
	

	public void reorderSelectTableQueryList(List<TableQueryModel> tableNames, DatabaseDetails dd) {
		final Map<String, Integer> orderRefMap = dd.getInsertionOrderRefMap();
		
		/*for(Map.Entry<String, Integer> e: orderRefMap.entrySet()){
			log.debug(e.getKey()+"->"+e.getValue());
		}*/
		
		Collections.sort(tableNames, new Comparator<TableQueryModel>() {
			public int compare(TableQueryModel o1, TableQueryModel o2) {
				return orderRefMap.get(o1.getTableName()) - orderRefMap.get(o2.getTableName());
			}
		});
		
		log.debug("sorted");
		for (TableQueryModel tqm : tableNames) {
			log.debug(tqm.getOrder());

		}

		int i = 0;
		for (TableQueryModel tqm : tableNames) {
			tqm.setOrder(i++);
		}
	}
	

	public void reorderDeleteTableQueryList(List<TableQueryModel> tableNames) {
		// sort by int order
		Collections.sort(tableNames, new Comparator<TableQueryModel>() {
			public int compare(TableQueryModel o1, TableQueryModel o2) {
				return o1.getOrder() - o2.getOrder();
			}
		});
		
		/*log.debug("sorted");
		for (TableQueryModel tqm : tableNames) {
			log.debug(tqm.getOrder());

		}*/

		// reverse the list
		Collections.reverse(tableNames);
		
		/*log.debug("after reverse");
		for (TableQueryModel tqm : tableNames) {
			log.debug(tqm.getOrder());

		}*/

		int i = 0;
		for (TableQueryModel tqm : tableNames) {
			tqm.setOrder(i++);
		}
	}
}
