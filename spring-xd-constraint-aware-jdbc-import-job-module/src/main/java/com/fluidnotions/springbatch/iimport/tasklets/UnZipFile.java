package com.fluidnotions.springbatch.iimport.tasklets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

public class UnZipFile implements Tasklet, InitializingBean{
	
	private static final Log log = LogFactory.getLog(UnZipFile.class);
	
	private String importZipFileName;
	private String jobKey;
	private String importDir;
	
	
	public void setImportZipFileName(String importZipFileName) {
		this.importZipFileName = importZipFileName;
	}


	public void setJobKey(String jobKey) {
		this.jobKey = jobKey;
	}


	public void setImportDir(String importDir) {
		this.importDir = importDir;
	}


	public void afterPropertiesSet() throws Exception {
		Assert.notNull(importZipFileName);	
		Assert.notNull(importDir);
		Assert.notNull(jobKey);
		/*throw new Exception("test restart after fail");*/
		
	}


	public RepeatStatus execute(StepContribution contribution,
			ChunkContext chunkContext) throws Exception {
		
		log.debug("importZipFileName: "+importZipFileName);
		log.debug("importDir: "+importDir);
//		DEBUG: com.fluidnotions.springbatch.iimport.tasklets.UnZipFile - importZipFileName: imports/uploaded/qLE10.zip
//		DEBUG: com.fluidnotions.springbatch.iimport.tasklets.UnZipFile - importDir: imports/temp
	
			 byte[] buffer = new byte[1024];
			
				
		String dstDir = importDir + File.separator + jobKey;
		log.debug("dstDir: "+dstDir);
		//create output directory is not exists
		File folder = new File(dstDir);
		if(!folder.exists()){
			folder.mkdirs();
		}

		//get the zip file content
		ZipInputStream zis = 
			new ZipInputStream(new FileInputStream(importZipFileName));
		//get the zipped file list entry
		ZipEntry ze = zis.getNextEntry();

		while(ze!=null){

		   String fileName = ze.getName();
	       File newFile = new File(dstDir + File.separator + fileName);

	       log.debug("file unzip : "+ newFile.getAbsoluteFile());

	       
	        FileOutputStream fos = new FileOutputStream(newFile);             

	        int len;
	        while ((len = zis.read(buffer)) > 0) {
	   		fos.write(buffer, 0, len);
	        }

	        fos.close();   
	        ze = zis.getNextEntry();
		}

	    zis.closeEntry();
		zis.close();

		log.debug("UnZipFile: Done");

    	return RepeatStatus.FINISHED;
	}

	

	

	
	
	

}
