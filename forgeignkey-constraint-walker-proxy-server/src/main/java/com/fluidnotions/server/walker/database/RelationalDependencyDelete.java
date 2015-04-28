package com.fluidnotions.server.walker.database;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fluidnotions.server.walker.WalkerUtil;
import com.fluidnotions.server.walker.model.TableQueryModel;

@Component
public class RelationalDependencyDelete {
	
	private static final Log log = LogFactory.getLog(RelationalDependencyDelete.class);
	
	@Autowired
	private WalkerUtil walkerUtil;
	
	@Autowired
	private DatabasesMetaDataMap databasesMetaDataMap;
	
	public void startSequentialDelete(String xdJobDefName, List<TableQueryModel> tableNames){
		walkerUtil.reorderDeleteTableQueryList(tableNames);
		for(TableQueryModel tqm: tableNames){
			databasesMetaDataMap.databaseMetaData(xdJobDefName).getJdbcTemplate().execute(tqm.getQuery());
		}
	}

	

}
