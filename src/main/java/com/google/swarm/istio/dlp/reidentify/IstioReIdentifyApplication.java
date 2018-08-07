package com.google.swarm.istio.dlp.reidentify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication

public class IstioReIdentifyApplication {

	private final Logger LOG = LoggerFactory
			.getLogger(IstioReIdentifyApplication.class);
	public static void main(String[] args) {
		SpringApplication.run(IstioReIdentifyApplication.class, args);
	}

}
