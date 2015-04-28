package com.fluidnotions.server.walker.model;

public class UploadedFile {
	
	private String name;
	private Integer size;
	private String url;
	private String thumbnail_url;
	private String delete_url;
	private String delete_type;
	
	private String jobKey;
	
	private String error_msg;
	
	public UploadedFile() {}
	
	public UploadedFile(String error_msg) {
		this(null, null, null, null, null, null, error_msg, null);
	}
	
	public UploadedFile(String name, Integer size, String jobKey) {
		this(name, size, null, null, null, null, null, jobKey);
	}
	
	
	public UploadedFile(String name, Integer size, String url,
			String thumbnail_url, String delete_url, String delete_type, String error_msg, String jobKey) {
		super();
		this.error_msg = error_msg;
		this.name = name;
		this.size = size;
		this.url = url;
		this.thumbnail_url = thumbnail_url;
		this.delete_url = delete_url;
		this.delete_type = delete_type;
		this.jobKey = jobKey;
	}

	public String getName() {
		return name;
	}

	public Integer getSize() {
		return size;
	}

	public String getUrl() {
		return url;
	}

	public String getThumbnail_url() {
		return thumbnail_url;
	}

	public String getDelete_url() {
		return delete_url;
	}

	public String getDelete_type() {
		return delete_type;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setSize(Integer size) {
		this.size = size;
	}

	public String getJobKey() {
		return jobKey;
	}

	public void setJobKey(String jobKey) {
		this.jobKey = jobKey;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setThumbnail_url(String thumbnail_url) {
		this.thumbnail_url = thumbnail_url;
	}

	public void setDelete_url(String delete_url) {
		this.delete_url = delete_url;
	}

	public void setDelete_type(String delete_type) {
		this.delete_type = delete_type;
	}

	public String getError_msg() {
		return error_msg;
	}

	public void setError_msg(String error_msg) {
		this.error_msg = error_msg;
	}
	
	

}
