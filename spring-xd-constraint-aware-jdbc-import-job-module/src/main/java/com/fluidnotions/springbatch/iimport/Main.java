package com.fluidnotions.springbatch.iimport;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {
	
	private static final Log log = LogFactory.getLog(Main.class);

	public static void main(String[] args){
		
		ApplicationContext context = new ClassPathXmlApplicationContext("spring-batch-context.xml");
		
		JobLauncher jobLauncher = (JobLauncher) context.getBean("jobLauncher");
		Job job = (Job) context.getBean("job");
		
		RunIdIncrementer runIdIncrementer = new RunIdIncrementer();
		runIdIncrementer.setKey(RandomStringUtils.randomAlphanumeric(5));
		
		try {
			long time_1 = System.currentTimeMillis();
			JobExecution execution = jobLauncher.run(job, runIdIncrementer.getNext(TestRunJobParametersFactory.buildJobParametersForTestRun(new JobParameters())));
			long time_2 = System.currentTimeMillis();
		
			log.info("Job Exit Status : "+ execution.getStatus() +", sec taken: "+((time_2-time_1)/1000) +", min taken: "+((time_2-time_1)/1000/60));
	 
		} catch (JobExecutionException e) {
			log.error("Job ExamResult failed", e);
		}/*finally{
			System.exit(0);
		}*/
	}
	
}
