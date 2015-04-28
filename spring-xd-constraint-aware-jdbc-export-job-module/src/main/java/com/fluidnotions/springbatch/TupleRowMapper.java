package com.fluidnotions.springbatch;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

public class TupleRowMapper implements RowMapper<Tuple>{

	@Override
	public Tuple mapRow(ResultSet rs, int rowNum) throws SQLException {
		TupleBuilder builder = TupleBuilder.tuple();

		for (int i=1; i <= rs.getMetaData().getColumnCount(); i++) {
			String name = JdbcUtils.lookupColumnName(rs.getMetaData(), i);
			builder.put(name, JdbcUtils.getResultSetValue(rs, i, String.class));
		}

		return builder.build();
	}

}
