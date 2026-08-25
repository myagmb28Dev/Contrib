package com.example.project.analysis.service;

import java.time.Instant;

import com.example.project.analysis.repository.AnalysisJobRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;

@Component
@ConditionalOnProperty(name = "app.analysis.dispatch-enabled", havingValue = "true", matchIfMissing = true)
public class AnalysisJobDispatcher {

    private final AnalysisJobRepository jobRepository;
    private final AnalysisJobWorker worker;

    public AnalysisJobDispatcher(AnalysisJobRepository jobRepository, AnalysisJobWorker worker) {
        this.jobRepository = jobRepository;
        this.worker = worker;
    }

    @Scheduled(fixedDelayString = "${app.analysis.dispatch-interval-ms:1000}")
    public void dispatchClaimableJobs() {
        jobRepository.findClaimableIds(Instant.now(), PageRequest.of(0, 20)).forEach(worker::process);
    }
}
