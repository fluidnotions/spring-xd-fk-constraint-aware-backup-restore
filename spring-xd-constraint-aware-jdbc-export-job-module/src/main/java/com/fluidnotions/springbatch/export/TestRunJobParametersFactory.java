package com.fluidnotions.springbatch.export;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

public class TestRunJobParametersFactory {

	
	public static JobParameters buildJobParametersForTestRun(JobParameters jp) {

		JobParametersBuilder jpb = new JobParametersBuilder(jp);

		jpb.addString("jobKey", "B76Md");
		return jpb.toJobParameters();

	}

}
