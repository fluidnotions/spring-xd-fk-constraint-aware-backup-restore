package com.fluidnotions.springbatch.export.extended;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;


public class JdbcItemReaderFactory implements FactoryBean<JdbcItemReader>, InitializingBean {

	private static final Log log = LogFactory
			.getLog(JdbcItemReaderFactory.class);

	private DataSource dataSource;

	private String sql;

	private boolean verifyCursorPosition = true;

	private boolean initialized = false;

	private JdbcItemReader reader;

	public JdbcItemReader getObject() throws Exception {

		if (!initialized) {
			throw new IllegalStateException(
					"Properties have not been initalized");
		}
		return reader;
	}

	public Class<?> getObjectType() {
		return JdbcItemReader.class;
	}

	public boolean isSingleton() {
		return true;
	}

	public void afterPropertiesSet() throws Exception {
		log.debug("sql: " + sql);
		
		//commenting out this together with statement settings 
		//in com.fluidnotions.springbatch.jdbcflatfile.extended.JdbcCursorItemReader<T> line 82
		//fixes 
		/*DatabaseType type = DatabaseType.fromMetaData(dataSource);

		switch (type) {
			case MYSQL:
				fetchSize = Integer.MIN_VALUE;
				// MySql doesn't support getRow for a streaming cursor
				verifyCursorPosition = false;
				break;
			case SQLITE:
				fetchSize = AbstractCursorItemReader.VALUE_NOT_SET;
				break;
			default:
				// keep configured fetchSize
		}*/

		reader = new JdbcItemReader();
		reader.setSql(sql);
		reader.setDataSource(dataSource);
		reader.setVerifyCursorPosition(verifyCursorPosition);
		reader.afterPropertiesSet();

		initialized = true;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	public void setVerifyCursorPosition(boolean verify) {
		this.verifyCursorPosition = verify;
	}


}
