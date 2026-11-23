package com.example.documentqa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Entry point for the REST API. Main (Console) and Evaluator still have their own main methods;
// this one starts an embedded Tomcat server instead of reading a question from the Console.
// @SpringBootApplication makes Spring scan this package for classes it should manage,
// such as HealthController, and configure the web server and JSON support.
@SpringBootApplication
public class DocumentQaApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumentQaApplication.class, args);
	}
}