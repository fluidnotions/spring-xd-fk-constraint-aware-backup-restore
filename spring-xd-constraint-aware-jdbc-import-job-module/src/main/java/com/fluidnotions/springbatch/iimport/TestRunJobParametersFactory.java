package com.fluidnotions.springbatch.iimport;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

public class TestRunJobParametersFactory {

	// order;tableName;sql,order;tableName;sql,order;tableName;sql,order;tableName;sql
	public static JobParameters buildJobParametersForTestRun(JobParameters jp) {

		JobParametersBuilder jpb = new JobParametersBuilder(jp);

		jpb.addString("importZipFileName", "vJ7vM.zip");
		jpb.addString("jobKey", "vJ7vM");
		jpb.addString("jobDef", "import1");
		
		return jpb.toJobParameters();

	}

}
