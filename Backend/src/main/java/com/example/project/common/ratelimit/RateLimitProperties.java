package com.example.project.common.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.api-rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private int publicLimit = 60;
    private int analysisLimit = 10;
    private int aiSummaryLimit = 5;
    private int windowSeconds = 60;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPublicLimit() {
        return publicLimit;
    }

    public void setPublicLimit(int publicLimit) {
        this.publicLimit = publicLimit;
    }

    public int getAnalysisLimit() {
        return analysisLimit;
    }

    public void setAnalysisLimit(int analysisLimit) {
        this.analysisLimit = analysisLimit;
    }

    public int getAiSummaryLimit() {
        return aiSummaryLimit;
    }

    public void setAiSummaryLimit(int aiSummaryLimit) {
        this.aiSummaryLimit = aiSummaryLimit;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }
}
