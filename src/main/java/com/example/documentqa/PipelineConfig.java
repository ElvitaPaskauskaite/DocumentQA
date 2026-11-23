package com.example.documentqa;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Tells Spring how to build the AnswerService the REST API uses. It goes through
// Pipeline.build, the same set-up as Main and Evaluator, so the API answers with
// exactly the system that was evaluated. Runs once at startup: documents are loaded
// and indexed before the first request, not on every question.
@Configuration
public class PipelineConfig {

	@Bean
	public AnswerService answerService() throws Exception {
		AppConfig config = new AppConfig("application.properties");
		return Pipeline.build(config).answerService();
	}
}