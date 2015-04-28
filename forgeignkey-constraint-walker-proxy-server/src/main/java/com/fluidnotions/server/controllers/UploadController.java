package com.fluidnotions.server.controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.fluidnotions.server.walker.database.DatabasesMetaDataMap;
import com.fluidnotions.server.walker.model.UploadedFile;

@Controller
public class UploadController {

	private static final Log log = LogFactory.getLog(UploadController.class);
	
	@Autowired
	private DatabasesMetaDataMap databasesMetaDataMap;

	@Value("${uploadUrlDirectoryBase}")
	private String uploadUrlDirectoryBase;
	
	@RequestMapping(value="/upload/{xdJobDefName}", method=RequestMethod.POST)
	public @ResponseBody List<UploadedFile> upload(
			@RequestParam("file") MultipartFile file, @PathVariable String xdJobDefName) {
		
		String importDef =  databasesMetaDataMap.databaseMetaData(xdJobDefName).getDefImport();
		
		List<UploadedFile> uploadedFiles = new ArrayList<UploadedFile>();
		
		// this identifies the group of report items for this job
		String jobKey = RandomStringUtils.randomAlphanumeric(5);
		UploadedFile u = new UploadedFile(file.getOriginalFilename(),
				Long.valueOf(file.getSize()).intValue(), jobKey);
		

		boolean handleUploadedFileError = false;
		try {
			InputStream inStream = file.getInputStream();
			
			if (!new File(uploadUrlDirectoryBase).exists()) {
				 u = new UploadedFile(
						"The specified upload location does not exist. Check application.properties value 'uploadUrlDirectoryBase'");
				 uploadedFiles.add(u);
				 return uploadedFiles;
			}

			if (!new File(uploadUrlDirectoryBase).canWrite()) {
				 u = new UploadedFile("The specified upload location is not writable. Please make sure the specified folder has the correct write permissions set for it.");
				 uploadedFiles.add(u);
				 return uploadedFiles;
			}

			//import def does not have unique location for each upload
			/*File uniqueJobDefUploadDir = new File(uploadUrlDirectoryBase + File.separator + importDef);
			if (!uniqueJobDefUploadDir.exists()) {
				uniqueJobDefUploadDir.mkdirs();
			}*/
			
			File uploadedFileDst = new File(uploadUrlDirectoryBase /*+ File.separator + importDef*/ + File.separator + file.getOriginalFilename());
			
			log.debug("file.getOriginalFilename(): " + file.getOriginalFilename());
			log.debug("file.getInputStream().available(): " + inStream.available());
			log.debug("uploadedFileDst: " + uploadedFileDst + " is writable: "
					+ uploadedFileDst.canWrite());

			
				OutputStream outStream = new FileOutputStream(uploadedFileDst);

				byte[] buffer = new byte[1024];

				int length;
				while ((length = inStream.read(buffer)) > 0) {
					outStream.write(buffer, 0, length);
				}
				outStream.flush();

				if (inStream != null)
					inStream.close();
				if (outStream != null)
					outStream.close();

			} catch (Throwable t) {
				t.printStackTrace();
				log.error("Throwable of class type: " + t.getClass().getName(), t);
				handleUploadedFileError  = true;
				u = new UploadedFile("Something went wrong with the file upload; please refresh the page and try again.");
			}

			if (handleUploadedFileError) {
				uploadedFiles.add(u);
				return uploadedFiles;
			}
		
		uploadedFiles.add(u);
		return uploadedFiles;
	}

}