package com.example.ai_notebook.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "openai")
public class OpenAiProperties {
    private String apiKey;
    private String baseUrl;
    private String model;

    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public void setModel(String model) { this.model = model; }
}
