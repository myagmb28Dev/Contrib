package com.example.project.analysis.service;

import java.util.List;
import java.util.UUID;

import com.example.project.analysis.ai.AiSummaryInput;
import com.example.project.analysis.ai.AiSummaryResult;
import com.example.project.analysis.calculator.ContributionScoreCalculator;
import com.example.project.analysis.calculator.ScoreResult;
import com.example.project.analysis.collector.CollectedActivity;
import com.example.project.analysis.collector.CollectedSnapshot;
import com.example.project.analysis.domain.ActivityEvent;
import com.example.project.analysis.domain.AnalysisJob;
import com.example.project.analysis.domain.ContributionAnalysis;
import com.example.project.analysis.domain.RepositorySnapshot;
import com.example.project.analysis.repository.ActivityEventRepository;
import com.example.project.analysis.repository.AnalysisJobRepository;
import com.example.project.analysis.repository.ContributionAnalysisRepository;
import com.example.project.analysis.repository.RepositorySnapshotRepository;
import com.example.project.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisTransactionService {

    private final AnalysisJobRepository jobRepository;
    private final RepositorySnapshotRepository snapshotRepository;
    private final ActivityEventRepository eventRepository;
    private final ContributionAnalysisRepository analysisRepository;
    private final ContributionScoreCalculator scoreCalculator;
    private final ObjectMapper objectMapper;

    public AnalysisTransactionService(AnalysisJobRepository jobRepository,
            RepositorySnapshotRepository snapshotRepository, ActivityEventRepository eventRepository,
            ContributionAnalysisRepository analysisRepository, ContributionScoreCalculator scoreCalculator,
            ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.snapshotRepository = snapshotRepository;
        this.eventRepository = eventRepository;
        this.analysisRepository = analysisRepository;
        this.scoreCalculator = scoreCalculator;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AnalysisJob startCollection(UUID jobId) {
        AnalysisJob job = detailedJob(jobId);
        job.getUser().getId();
        job.getRepository().getName();
        job.startCollection();
        return job;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID storeSnapshot(UUID jobId, long subjectGithubId, CollectedSnapshot collected) {
        AnalysisJob job = detailedJob(jobId);
        RepositorySnapshot snapshot = snapshotRepository.save(RepositorySnapshot.create(job, subjectGithubId,
                collected.collectedAt(), collected.sourceMetadata(), collected.snapshotHash()));
        List<ActivityEvent> events = collected.activities().stream().map(activity -> toEntity(snapshot, activity)).toList();
        eventRepository.saveAll(events);
        return snapshot.getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CalculatedAnalysis calculate(UUID jobId, UUID snapshotId) {
        AnalysisJob job = detailedJob(jobId);
        job.startAnalysis();
        RepositorySnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new ResourceNotFoundException("Snapshot not found"));
        List<ActivityEvent> events = eventRepository.findAllBySnapshotIdOrderByOccurredAtAscExternalIdAsc(snapshotId);
        ScoreResult result = scoreCalculator.calculate(events);
        ContributionAnalysis analysis = ContributionAnalysis.create(snapshot, json(result.metrics()), result.score(),
                result.scoreVersion(), result.calculationRules(), "[]");
        analysisRepository.save(analysis);
        AiSummaryInput input = new AiSummaryInput(job.getRepository().getFullName(),
                job.getRepository().getLanguage(), result.metrics(), result.score());
        return new CalculatedAnalysis(analysis.getId(), input);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyAiAndComplete(UUID jobId, UUID analysisId, AiSummaryResult result) {
        AnalysisJob job = detailedJob(jobId);
        job.startAiProcessing();
        ContributionAnalysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found"));
        analysis.applyAiSummary(result.summary(), result.model(), result.promptVersion(), json(result.technicalAreas()));
        job.complete();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(UUID jobId, Throwable throwable) {
        jobRepository.findById(jobId).ifPresent(job -> job.fail(
                throwable.getClass().getSimpleName(), throwable.getMessage()));
    }

    private AnalysisJob detailedJob(UUID jobId) {
        return jobRepository.findDetailedById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis job not found"));
    }

    private ActivityEvent toEntity(RepositorySnapshot snapshot, CollectedActivity activity) {
        return ActivityEvent.create(snapshot, activity.externalId(), activity.type(), activity.authorGithubId(),
                activity.occurredAt(), activity.title(), activity.state(), activity.additions(),
                activity.deletions(), activity.changedFiles(), activity.rawPayload());
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize analysis data", exception);
        }
    }
}
