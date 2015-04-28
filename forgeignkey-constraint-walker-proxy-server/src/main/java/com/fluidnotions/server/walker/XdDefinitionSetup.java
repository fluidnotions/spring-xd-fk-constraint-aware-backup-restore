package com.fluidnotions.server.walker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;

import com.fluidnotions.server.walker.database.DatabaseDetails;

@Service
public class XdDefinitionSetup {

	private static final Log log = LogFactory.getLog(XdDefinitionSetup.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Value("${xdserverurl}")
	private String xdserverurl;

	@Value("${spring.datasource.url}")
	private String xdDatasourceUrl;

	@Value("${spring.datasource.username}")
	private String xdDatasourceUsername;

	@Value("${spring.datasource.password}")
	private String xdDatasourcePassword;
	
	@Value("${uploadUrlDirectoryBase}")
	private String uploadUrlDirectoryBase;
	
	protected HttpStatus clientHttpResponseStatusCode;

	public void checkAllXdDefSetup(
			Map<String, DatabaseDetails> databaseMetaDataMap) {

		this.clientHttpResponseStatusCode = null;

		for (Map.Entry<String, DatabaseDetails> e : databaseMetaDataMap
				.entrySet()) {
			log.debug("checkXdJobDef for: " + e.getKey());
			checkXdExportJobDef(e.getValue());
			checkXdExportStreamDef(e.getValue());
			
			checkXdImportJobDef(e.getValue());
			checkXdImportStreamDef(e.getValue());
			
		}
	}

	public void checkXdExportJobDef(DatabaseDetails dd) {
		//only create if there is an exportDir field
		if(dd.isImportOnly()) return;
		
		boolean createNewDef = false;

		String end1 = xdserverurl + "/jobs/definitions/";

		ResponseEntity<LinkedHashMap> response = null;
		try {
			response = initRestTemplate().getForEntity(end1
					+ dd.getDefExport(), LinkedHashMap.class);
		} catch (Exception e1) {
			log.warn(e1.getMessage());
		}
		
		if (clientHttpResponseStatusCode != null
				&& clientHttpResponseStatusCode.equals(HttpStatus.NOT_FOUND)) {
			createNewDef = true;
		} else {
			clientHttpResponseStatusCode = null;
			LinkedHashMap<String, String> rspMap = response.getBody();
			if (rspMap.containsKey("logref")
					&& rspMap.get("logref").equals("NoSuchDefinitionException")) {
				createNewDef = true;
			} else {
				String name = rspMap.get("name");
				String status = rspMap.get("status");
				String definition = rspMap.get("definition");

				log.debug("name: " + name + ", status: " + status
						+ ", definition: " + definition);

				boolean match = false;
				try {
					match = dd.checkXdExportJobDefinitionAgaints(definition);
				} catch (Exception e) {
					e.printStackTrace();
					log.error("checkXdJobDefinitionAgaints error", e);
				}
				if (!match) {
					log.debug("doesn't match proxy version removing def from xd: REST delete: "+end1 + name);
					// destroy existing
					try {
						new RestTemplate().delete(end1 + name);
						//need to give xd time to process since we are about to create one with the same name
						Thread.sleep(3000);
						createNewDef = true;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		if (createNewDef) {

			log.debug("EXPORT: name: " + dd.getDefExport() + ", creating new def: "
					+ dd.createXdExportJobDefString());
			postToXD(initRestTemplate(), dd.getDefExport(), dd.createXdExportJobDefString(), end1);
		}

	}
	
	public void checkXdExportStreamDef(DatabaseDetails dd) {
		//only create if there is an exportDir field
		if(dd.isImportOnly()) return;
		
		boolean createNewDef = false;

		String end1 = xdserverurl + "/streams/definitions/";

		ResponseEntity<LinkedHashMap> response = null;
		try {
			response = initRestTemplate().getForEntity(end1
					+ dd.getDefExport() + "tap", LinkedHashMap.class);
		} catch (Exception e1) {
			log.warn(e1.getMessage());
		}
		if (clientHttpResponseStatusCode != null
				&& clientHttpResponseStatusCode.equals(HttpStatus.NOT_FOUND)) {
			log.debug("this.clientHttpResponseStatusCode: "
					+ clientHttpResponseStatusCode.toString());
			createNewDef = true;
		} else {
			
			LinkedHashMap<String, String> rspMap = response.getBody();

			if (rspMap.containsKey("logref")
					&& rspMap.get("logref").equals("NoSuchDefinitionException")) {
				createNewDef = true;
			} else {
				String name = rspMap.get("name");
				String status = rspMap.get("status");
				String definition = rspMap.get("definition");

				log.debug("name: " + name + ", status: " + status
						+ ", definition: " + definition);

				boolean match = dd.checkXdExportStreamDefinitionAgaints(definition,
						this.xdDatasourceUrl, this.xdDatasourceUsername,
						this.xdDatasourcePassword);
				if (!match) {
					log.debug("doesn't match proxy version removing def from xd: REST delete: "+end1 + name);
					// destroy existing
					try {
						new RestTemplate().delete(end1 + name);
						//need to give xd time to process since we are about to create one with the same name
						Thread.sleep(3000);
						createNewDef = true;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		if (createNewDef) {
			log.debug("EXPORT STREAM TAP: name: "
					+ dd.getDefExport()
					+ "tap"
					+ ", creating new def: "
					+ dd.createXdExportStreamDefString(this.xdDatasourceUrl,
							this.xdDatasourceUsername,
							this.xdDatasourcePassword));
			postToXD(initRestTemplate(), dd.getDefExport() + "tap",
					dd.createXdExportStreamDefString(this.xdDatasourceUrl,
							this.xdDatasourceUsername,
							this.xdDatasourcePassword), end1);
		}

	}
	
	//for the import definition name we are just using export def + suffix "import" to reduce required refactoring
	public void checkXdImportJobDef(DatabaseDetails dd) {
		//only create if there is an importDir field
		if(dd.isExportOnly()) return;
		
		boolean createNewDef = false;


		String end1 = xdserverurl + "/jobs/definitions/";

		ResponseEntity<LinkedHashMap> response = null;
		try {
			response = initRestTemplate().getForEntity(end1
					+ dd.getDefImport(), LinkedHashMap.class);
		} catch (Exception e1) {
			log.warn(e1.getMessage());
		}
		
		if (clientHttpResponseStatusCode != null
				&& clientHttpResponseStatusCode.equals(HttpStatus.NOT_FOUND)) {
			createNewDef = true;
		} else {
			clientHttpResponseStatusCode = null;
			LinkedHashMap<String, String> rspMap = response.getBody();
			if (rspMap.containsKey("logref")
					&& rspMap.get("logref").equals("NoSuchDefinitionException")) {
				createNewDef = true;
			} else {
				String name = rspMap.get("name");
				String status = rspMap.get("status");
				String definition = rspMap.get("definition");

				log.debug("name: " + name + ", status: " + status
						+ ", definition: " + definition);

				boolean match = false;
				try {
					match = dd.checkXdImportJobDefinitionAgaints(definition);
				} catch (Exception e) {
					e.printStackTrace();
					log.error("checkXdJobDefinitionAgaints error", e);
				}
				if (!match) {
					log.debug("doesn't match proxy version removing def from xd: REST delete: "+end1 + name);
					// destroy existing
					try {
						new RestTemplate().delete(end1 + name);
						//need to give xd time to process since we are about to create one with the same name
						Thread.sleep(3000);
						createNewDef = true;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		if (createNewDef) {

			log.debug("IMPORT: name: " + dd.getDefImport() + ", creating new def: "
					+ dd.createXdImportJobDefString(this.uploadUrlDirectoryBase));
			postToXD(initRestTemplate(), dd.getDefImport(), dd.createXdImportJobDefString(this.uploadUrlDirectoryBase), end1);
		}

	}

	public void checkXdImportStreamDef(DatabaseDetails dd) {
		//only create if there is an importDir field
		if(dd.isExportOnly()) return;
		
		boolean createNewDef = false;

		String end1 = xdserverurl + "/streams/definitions/";

		ResponseEntity<LinkedHashMap> response = null;
		try {
			response = initRestTemplate().getForEntity(end1
					+ dd.getDefImport() + "tap", LinkedHashMap.class);
		} catch (Exception e1) {
			log.warn(e1.getMessage());
		}
		if (clientHttpResponseStatusCode != null
				&& clientHttpResponseStatusCode.equals(HttpStatus.NOT_FOUND)) {
			log.debug("this.clientHttpResponseStatusCode: "
					+ clientHttpResponseStatusCode.toString());
			createNewDef = true;
		} else {
			
			LinkedHashMap<String, String> rspMap = response.getBody();

			if (rspMap.containsKey("logref")
					&& rspMap.get("logref").equals("NoSuchDefinitionException")) {
				createNewDef = true;
			} else {
				String name = rspMap.get("name");
				String status = rspMap.get("status");
				String definition = rspMap.get("definition");

				log.debug("name: " + name + ", status: " + status
						+ ", definition: " + definition);

				boolean match = dd.checkXdImportStreamDefinitionAgaints(definition,
						this.xdDatasourceUrl, this.xdDatasourceUsername,
						this.xdDatasourcePassword);
				if (!match) {
					log.debug("doesn't match proxy version removing def from xd: REST delete: "+end1 + name);
					// destroy existing
					try {
						new RestTemplate().delete(end1 + name);
						//need to give xd time to process since we are about to create one with the same name
						Thread.sleep(3000);
						createNewDef = true;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		if (createNewDef) {
			log.debug("IMPORT STREAM TAP: name: "
					+ dd.getDefImport()
					+ "tap"
					+ ", creating new def: "
					+ dd.createXdImportStreamDefString(this.xdDatasourceUrl,
							this.xdDatasourceUsername,
							this.xdDatasourcePassword));
			postToXD(initRestTemplate(), dd.getDefImport() + "tap",
					dd.createXdImportStreamDefString(this.xdDatasourceUrl,
							this.xdDatasourceUsername,
							this.xdDatasourcePassword), end1);
		}

	}

	private void postToXD(RestTemplate restTemplate, String name,
			String definition, String url) {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("name", name);
		map.add("definition", definition);
		log.debug("postToXD: name: "+name+", definition: "+definition);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(
				map, headers);

		restTemplate.getMessageConverters().add(new FormHttpMessageConverter());
		boolean ex = false;
		try {
			restTemplate.postForObject(url, request, Object.class);
		} catch (Exception e) {
			ex = true;
			log.error("problem with postToXD " + url, e);
		}
		log.debug("postToXD completed ex="+ex);
	}

	private RestTemplate initRestTemplate() {
		clientHttpResponseStatusCode = null;
		RestTemplate restTemplate = new RestTemplate();
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
		messageConverters.addAll(restTemplate.getMessageConverters());
		messageConverters.add(new FormHttpMessageConverter());
		restTemplate.setMessageConverters(messageConverters);
		restTemplate.setErrorHandler(new ResponseErrorHandler() {

			@Override
			public boolean hasError(ClientHttpResponse response)
					throws IOException {

				HttpStatus statusCode = getHttpStatusCode(response);
				return (statusCode.series() == HttpStatus.Series.CLIENT_ERROR || statusCode
						.series() == HttpStatus.Series.SERVER_ERROR);
			}

			@Override
			public void handleError(ClientHttpResponse response)
					throws IOException {
				clientHttpResponseStatusCode = getHttpStatusCode(response);
				switch (clientHttpResponseStatusCode.series()) {
				case CLIENT_ERROR:
					break;
				case SERVER_ERROR:
					throw new HttpServerErrorException(
							clientHttpResponseStatusCode, response
									.getStatusText(), response.getHeaders(),
							getResponseBody(response), getCharset(response));
				default:
					throw new RestClientException("Unknown status code ["
							+ clientHttpResponseStatusCode + "]");
				}

			}

		});
		return restTemplate;
	}

	private HttpStatus getHttpStatusCode(ClientHttpResponse response)
			throws IOException {
		HttpStatus statusCode;
		try {
			statusCode = response.getStatusCode();
		} catch (IllegalArgumentException ex) {
			throw new UnknownHttpStatusCodeException(
					response.getRawStatusCode(), response.getStatusText(),
					response.getHeaders(), getResponseBody(response),
					getCharset(response));
		}
		return statusCode;
	}

	private byte[] getResponseBody(ClientHttpResponse response) {
		try {
			InputStream responseBody = response.getBody();
			if (responseBody != null) {
				return FileCopyUtils.copyToByteArray(responseBody);
			}
		} catch (IOException ex) {
			// ignore
		}
		return new byte[0];
	}

	private Charset getCharset(ClientHttpResponse response) {
		HttpHeaders headers = response.getHeaders();
		MediaType contentType = headers.getContentType();
		return contentType != null ? contentType.getCharSet() : null;
	}

}
