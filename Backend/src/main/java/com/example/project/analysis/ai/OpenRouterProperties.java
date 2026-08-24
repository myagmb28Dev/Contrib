package com.example.project.analysis.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai.openrouter")
public class OpenRouterProperties {

    private String apiKey = "";
    private String baseUrl = "https://openrouter.ai/api/v1";
    private String model = "nvidia/nemotron-3-super-120b-a12b:free";
    private String siteUrl = "";
    private String appName = "Contrib";

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getSiteUrl() { return siteUrl; }
    public void setSiteUrl(String siteUrl) { this.siteUrl = siteUrl; }
    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }
}
