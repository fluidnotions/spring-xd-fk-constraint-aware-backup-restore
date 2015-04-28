package com.fluidnotions.springbatch.export.extended;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;



public class JdbcItemReader extends JdbcCursorItemReader<Tuple>{

	@Override
	public void afterPropertiesSet() throws Exception {
		setRowMapper(new RowMapper<Tuple>() {
			public Tuple mapRow(ResultSet rs, int rowNum) throws SQLException {
				
				//TupleBuilder builder = TupleBuilder.tuple();
				TupleBuilder builder = new TupleBuilder();
				builder.setNumberFormatFromLocale(Locale.US);
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				dateFormat.setLenient(false);
				builder.setDateFormat(dateFormat);
				//add tableName to each record so it can be used in the import/restore process
				//this functionality was in TuplePassThroughLineAggregator but since tuples are
				//immutable it then has to be recreated which is unnecessary overhead
				builder.put("tableName", rs.getMetaData().getTableName(1));

				for (int i=1; i <= rs.getMetaData().getColumnCount(); i++) {
					String name = JdbcUtils.lookupColumnName(rs.getMetaData(), i);
					builder.put(name, JdbcUtils.getResultSetValue(rs, i, String.class));
				}

				return builder.build();
			}
		});

		super.afterPropertiesSet();
	}
	
	

	
}
