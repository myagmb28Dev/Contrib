package com.example.project.analysis.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.example.project.analysis.domain.AnalysisJob;
import com.example.project.analysis.dto.AnalysisJobResponse;
import com.example.project.analysis.dto.AnalysisResponse;
import com.example.project.analysis.repository.AnalysisJobRepository;
import com.example.project.analysis.repository.ContributionAnalysisRepository;
import com.example.project.analysis.repository.RepositorySnapshotRepository;
import com.example.project.analysis.repository.ActivityEventRepository;
import com.example.project.analysis.ai.AiSummaryInput;
import com.example.project.analysis.ai.AiSummaryService;
import com.example.project.auth.domain.User;
import com.example.project.auth.repository.UserRepository;
import com.example.project.common.exception.ResourceNotFoundException;
import com.example.project.repository.domain.GitHubRepository;
import com.example.project.repository.service.RepositoryService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisService {

    public static final String COLLECTOR_VERSION = "github-v1";

    private final AnalysisJobRepository jobRepository;
    private final ContributionAnalysisRepository analysisRepository;
    private final UserRepository userRepository;
    private final RepositoryService repositoryService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final AiSummaryService aiSummaryService;
    private final RepositorySnapshotRepository snapshotRepository;
    private final ActivityEventRepository eventRepository;

    public AnalysisService(AnalysisJobRepository jobRepository,
            ContributionAnalysisRepository analysisRepository, UserRepository userRepository,
            RepositoryService repositoryService, ApplicationEventPublisher eventPublisher, ObjectMapper objectMapper,
            AiSummaryService aiSummaryService, RepositorySnapshotRepository snapshotRepository,
            ActivityEventRepository eventRepository) {
        this.jobRepository = jobRepository;
        this.analysisRepository = analysisRepository;
        this.userRepository = userRepository;
        this.repositoryService = repositoryService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.aiSummaryService = aiSummaryService;
        this.snapshotRepository = snapshotRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public AnalysisJobResponse create(UUID userId, UUID repositoryId, Instant periodStart, Instant periodEnd) {
        GitHubRepository repository = repositoryService.getOwnedRepository(userId, repositoryId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var existing = jobRepository.findByUserIdAndRepositoryIdAndPeriodStartAndPeriodEndAndCollectorVersion(
                userId, repositoryId, periodStart, periodEnd, COLLECTOR_VERSION);
        if (existing.isPresent()) {
            return AnalysisJobResponse.from(existing.get());
        }
        AnalysisJob job = jobRepository.save(AnalysisJob.create(user, repository, periodStart, periodEnd, COLLECTOR_VERSION));
        eventPublisher.publishEvent(new AnalysisJobCreatedEvent(job.getId()));
        return AnalysisJobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public AnalysisJobResponse getJob(UUID userId, UUID jobId) {
        AnalysisJob job = jobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis job not found"));
        return AnalysisJobResponse.from(job);
    }

    @Transactional
    public AnalysisJobResponse retry(UUID userId, UUID jobId) {
        AnalysisJob job = jobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis job not found"));
        snapshotRepository.findByAnalysisJobId(jobId).ifPresent(snapshot -> {
            analysisRepository.findBySnapshotId(snapshot.getId()).ifPresent(analysisRepository::delete);
            eventRepository.deleteAllBySnapshotId(snapshot.getId());
            snapshotRepository.delete(snapshot);
        });
        job.retry();
        eventPublisher.publishEvent(new AnalysisJobCreatedEvent(job.getId()));
        return AnalysisJobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public AnalysisResponse getAnalysis(UUID userId, UUID analysisId) {
        return AnalysisResponse.from(analysisRepository.findByIdAndSnapshotAnalysisJobUserId(analysisId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found")), objectMapper);
    }

    @Transactional(readOnly = true)
    public List<AnalysisResponse> listForRepository(UUID userId, UUID repositoryId) {
        repositoryService.getOwnedRepository(userId, repositoryId);
        return analysisRepository
                .findAllBySnapshotRepositoryIdAndSnapshotAnalysisJobUserIdOrderByCreatedAtDesc(repositoryId, userId)
                .stream().map(analysis -> AnalysisResponse.from(analysis, objectMapper)).toList();
    }

    @Transactional(readOnly = true)
    public List<AnalysisResponse> list(UUID userId) {
        return analysisRepository.findAllBySnapshotAnalysisJobUserIdOrderByCreatedAtDesc(userId)
                .stream().map(analysis -> AnalysisResponse.from(analysis, objectMapper)).toList();
    }

    @Transactional
    public AnalysisResponse regenerateSummary(UUID userId, UUID analysisId) {
        var analysis = analysisRepository.findByIdAndSnapshotAnalysisJobUserId(analysisId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found"));
        var metrics = objectMapper.convertValue(readTree(analysis.getMetrics()),
                com.example.project.analysis.calculator.AnalysisMetrics.class);
        var input = new AiSummaryInput(analysis.getSnapshot().getRepository().getFullName(),
                analysis.getSnapshot().getRepository().getLanguage(), metrics, analysis.getScore());
        var result = aiSummaryService.summarize(input);
        analysis.applyAiSummary(result.summary(), result.model(), result.promptVersion(),
                writeJson(result.technicalAreas()));
        return AnalysisResponse.from(analysis, objectMapper);
    }

    private com.fasterxml.jackson.databind.JsonNode readTree(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored analysis JSON is invalid", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize AI summary", exception);
        }
    }
}
