package com.example.project.analysis.service;

import java.util.UUID;

import com.example.project.analysis.ai.AiSummaryService;
import com.example.project.analysis.ai.AiSummaryResult;
import com.example.project.analysis.collector.CollectedSnapshot;
import com.example.project.analysis.collector.GitHubActivityCollector;
import com.example.project.analysis.domain.AnalysisJob;
import com.example.project.auth.domain.GitHubAccount;
import com.example.project.auth.repository.GitHubAccountRepository;
import com.example.project.common.exception.ResourceNotFoundException;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AnalysisJobWorker {

    private final AnalysisTransactionService transactions;
    private final GitHubActivityCollector collector;
    private final GitHubAccountRepository accountRepository;
    private final AiSummaryService aiSummaryService;

    public AnalysisJobWorker(AnalysisTransactionService transactions, GitHubActivityCollector collector,
            GitHubAccountRepository accountRepository, AiSummaryService aiSummaryService) {
        this.transactions = transactions;
        this.collector = collector;
        this.accountRepository = accountRepository;
        this.aiSummaryService = aiSummaryService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void process(AnalysisJobCreatedEvent event) {
        UUID jobId = event.jobId();
        try {
            AnalysisJob job = transactions.startCollection(jobId);
            GitHubAccount account = accountRepository.findByUserId(job.getUser().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("GitHub account not found"));
            CollectedSnapshot collected = collector.collect(job, account.getGithubUserId());
            UUID snapshotId = transactions.storeSnapshot(jobId, account.getGithubUserId(), collected);
            CalculatedAnalysis analysis = transactions.calculate(jobId, snapshotId);
            AiSummaryResult summary = aiSummaryService.summarize(analysis.aiInput());
            transactions.applyAiAndComplete(jobId, analysis.analysisId(), summary);
        } catch (Exception exception) {
            transactions.fail(jobId, exception);
        }
    }
}
