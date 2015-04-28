package com.fluidnotions.springbatch.export.tasklets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

public class ZipFile implements Tasklet, InitializingBean{
	
	private static final Log log = LogFactory.getLog(ZipFile.class);
	
	//<property name="resource" value="file:csv/joined/export-file-all.csv" />
	private Resource directory;
	private String zippedFilePath;

	 
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(directory);
		Assert.notNull(zippedFilePath);		
		/*throw new Exception("test restart after fail");*/
		
	}

	public RepeatStatus execute(StepContribution contribution,
			ChunkContext chunkContext) throws Exception {
		
		
		log.debug("joinFlatFileResource: "+directory.getFile().getPath()); 
		log.debug("zippedFilePath: "+zippedFilePath);
	
		
		
		
		byte[] buffer = new byte[1024];
    	try{
    		
    		File zippedFileOutputPath = new File(zippedFilePath);
    		//check dir exists and create if it doesn't
    		File zippedFileDir = new File(zippedFileOutputPath.getParent());
    		if(!zippedFileDir.exists()){
    			zippedFileDir.mkdirs();
    			
    		}
    		
    		FileOutputStream fos = new FileOutputStream(zippedFileOutputPath);
    		ZipOutputStream zos = new ZipOutputStream(fos);
    		
    		File dir = directory.getFile();
			log.debug("zipping directory files: "+dir.getPath()); 
			Assert.state(dir.isDirectory());
		 
			File[] files = dir.listFiles();
			
			for(File f: files){
    		
	    		FileInputStream in = new FileInputStream(f);
	    		ZipEntry ze= new ZipEntry(f.getName());
	    		log.debug("zipEntryName: "+f.getName());
	    		zos.putNextEntry(ze);
	    		
	    		int len;
	    		while ((len = in.read(buffer)) > 0) {
	    			zos.write(buffer, 0, len);
	    		}
	 
	    		in.close();
	    		zos.closeEntry();
    		
			}
    		
    		
 
    		//remember close it
    		zos.close();
 
    		log.debug("Done");
 
    	}catch(IOException ex){
    	   log.error("IO issue on ZipFileTasklet", ex);
    	}
    	
    	return RepeatStatus.FINISHED;
	}

	public Resource getJoinFlatFileResource() {
		return directory;
	}

	public void setJoinFlatFileResource(Resource joinFlatFileResource) {
		this.directory = joinFlatFileResource;
	}

	public String getZippedFilePath() {
		return zippedFilePath;
	}

	public void setZippedFilePath(String zippedFilePath) {
		this.zippedFilePath = zippedFilePath;
	}

	public Resource getDirectory() {
		return directory;
	}

	public void setDirectory(Resource directory) {
		this.directory = directory;
	}

	

	
	
	

}
