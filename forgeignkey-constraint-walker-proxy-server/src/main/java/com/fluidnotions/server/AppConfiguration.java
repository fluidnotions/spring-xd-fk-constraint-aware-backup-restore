package com.fluidnotions.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfiguration implements EmbeddedServletContainerCustomizer {

	@Value("${port}")
    private String port;

    @Override
	public void customize(ConfigurableEmbeddedServletContainer factory) {
    	factory.setPort(new Integer(port));		
	}
	
}