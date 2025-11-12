package com.example.ai_notebook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class AiNotebookApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiNotebookApplication.class, args);
	}

}
