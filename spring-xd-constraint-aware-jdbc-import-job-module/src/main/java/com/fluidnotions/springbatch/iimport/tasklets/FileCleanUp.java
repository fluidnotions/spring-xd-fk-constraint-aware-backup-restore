package com.fluidnotions.springbatch.iimport.tasklets;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

public class FileCleanUp implements Tasklet, InitializingBean {
	
	 private static final Log log = LogFactory.getLog(FileCleanUp.class);
	 
	  private Resource directory;
	  private String directories;//comma separated paths
	  private String deleteFolder;//true/false
	 
	  public void afterPropertiesSet() throws Exception {
		Assert.isTrue(directory!=null||directories!=null, "directory/directories must be set");
	  }
	 
	  
	  public RepeatStatus execute(StepContribution contribution, 
	               ChunkContext chunkContext) throws Exception {
		  
		 if(directory!=null){ 
			File dir = directory.getFile();
			log.debug("deleting directory: "+dir.getPath()); 
			Assert.state(dir.isDirectory());
		 
			File[] files = dir.listFiles();
			deleteFiles(files);
			
			 if(this.deleteFolder.trim().equalsIgnoreCase("true")){
				 deleteEmptyDirectory(dir);
			 }
		 }else{
			log.debug("deleting directories: "+directories); 
			String[] directoriesArray = directories.split(",");
			for(String dir: directoriesArray){
				File f = new File(dir);
				Assert.state(f.isDirectory());
				
				File[] files = f.listFiles();
				deleteFiles(files);
				
				 if(this.deleteFolder.trim().equalsIgnoreCase("true")){
					 deleteEmptyDirectory(f);
				 }
				
			}
		 }
		
		return RepeatStatus.FINISHED;
	  }


	private void deleteFiles(File[] files) {
		for (int i = 0; i < files.length; i++) {
		  boolean deleted = files[i].delete();
		  if (!deleted) {
			throw new UnexpectedJobExecutionException(
	                       "Could not delete file " + files[i].getPath());
		  } else {
		       log.debug(files[i].getPath() + " is deleted!");
		  }
		}
	}
	
	private void deleteEmptyDirectory(File dir) {
		
		  if (dir.listFiles().length==0) {
			boolean deleted = dir.delete();
			if (!deleted) {
				throw new UnexpectedJobExecutionException(
						"Could not delete file " + dir.getPath());
			} else {
				log.debug(dir.getPath() + " is deleted!");
			}
		}else{
			log.error("Directory "+dir.getPath() + " contains files, delete failed!");
		}
	
	}
	 
	  public Resource getDirectory() {
		return directory;
	  }
	 
	  public void setDirectory(Resource directory) {
		this.directory = directory;
	  }


	public String getDirectories() {
		return directories;
	}


	public void setDirectories(String directories) {
		this.directories = directories;
	}


	public String getDeleteFolder() {
		return deleteFolder;
	}


	public void setDeleteFolder(String deleteFolder) {
		this.deleteFolder = deleteFolder;
	}
	  
	  
	 

}
