package com.example.docusign_integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan("com.example.docusign_integration.servlet")
public class DocusignIntegrationApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocusignIntegrationApplication.class, args);
	}

}
