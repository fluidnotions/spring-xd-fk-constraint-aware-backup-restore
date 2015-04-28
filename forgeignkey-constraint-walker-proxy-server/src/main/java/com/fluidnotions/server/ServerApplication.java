package com.fluidnotions.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan("com.fluidnotions")
@EnableAutoConfiguration
public class ServerApplication extends SpringBootServletInitializer {

	private static final Log log = LogFactory
			.getLog(ServerApplication.class);

	@Override
	protected SpringApplicationBuilder configure(
			SpringApplicationBuilder application) {
		return application.sources(ServerApplication.class);
	}


	public static void main(String[] args) {
		SpringApplication.run(ServerApplication.class, args);
	}
}
