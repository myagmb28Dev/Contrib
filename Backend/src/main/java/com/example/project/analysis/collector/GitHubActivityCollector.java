package com.example.project.analysis.collector;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.project.analysis.domain.ActivityType;
import com.example.project.analysis.domain.AnalysisJob;
import com.example.project.auth.service.GitHubAccessTokenService;
import com.example.project.github.client.GitHubApiClient;
import com.example.project.github.dto.GitHubCommitDetailDto;
import com.example.project.github.dto.GitHubCommitDto;
import com.example.project.github.dto.GitHubPullRequestDto;
import com.example.project.github.dto.GitHubReviewDto;
import com.example.project.github.dto.GitHubUserDto;
import com.example.project.repository.domain.GitHubRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

@Component
public class GitHubActivityCollector {

    private final GitHubApiClient githubApiClient;
    private final GitHubAccessTokenService accessTokenService;
    private final ObjectMapper objectMapper;

    public GitHubActivityCollector(GitHubApiClient githubApiClient,
            GitHubAccessTokenService accessTokenService, ObjectMapper objectMapper) {
        this.githubApiClient = githubApiClient;
        this.accessTokenService = accessTokenService;
        this.objectMapper = objectMapper;
    }

    public CollectedSnapshot collect(AnalysisJob job, long subjectGithubId) {
        GitHubRepository repository = job.getRepository();
        String token = accessTokenService.getValidAccessToken(job.getUser().getId());
        List<CollectedActivity> activities = new ArrayList<>();

        collectCommits(token, repository, job, subjectGithubId, activities);
        List<GitHubPullRequestDto> pulls = githubApiClient.getPullRequests(
                token, repository.getOwnerLogin(), repository.getName());
        collectPullRequests(pulls, job, subjectGithubId, activities);
        collectReviews(token, repository, pulls, job, subjectGithubId, activities);

        activities.sort(Comparator.comparing(CollectedActivity::occurredAt)
                .thenComparing(activity -> activity.type().name())
                .thenComparing(CollectedActivity::externalId));

        Instant collectedAt = Instant.now();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", "github");
        metadata.put("repository", repository.getFullName());
        metadata.put("branch", repository.getDefaultBranch());
        metadata.put("scope", "default-branch commits; authored pull requests and reviews");
        metadata.put("subjectGithubId", subjectGithubId);
        metadata.put("periodStart", job.getPeriodStart().toString());
        metadata.put("periodEnd", job.getPeriodEnd().toString());
        metadata.put("collectorVersion", job.getCollectorVersion());
        metadata.put("activityCount", activities.size());

        String metadataJson = json(metadata);
        Map<String, Object> snapshotPayload = new LinkedHashMap<>();
        snapshotPayload.put("metadata", metadata);
        snapshotPayload.put("activities", activities);
        String snapshotJson = json(snapshotPayload);
        return new CollectedSnapshot(collectedAt, metadataJson, sha256(snapshotJson), List.copyOf(activities));
    }

    private void collectCommits(String token, GitHubRepository repository, AnalysisJob job,
            long subjectGithubId, List<CollectedActivity> target) {
        List<GitHubCommitDto> commits = githubApiClient.getCommits(token, repository.getOwnerLogin(),
                repository.getName(), repository.getDefaultBranch(), job.getPeriodStart(), job.getPeriodEnd());
        for (GitHubCommitDto commit : commits) {
            if (!isSubject(commit.author(), subjectGithubId)
                    || commit.parents() == null || commit.parents().size() > 1
                    || commit.commit() == null || commit.commit().author() == null
                    || !inside(commit.commit().author().date(), job)) {
                continue;
            }
            GitHubCommitDetailDto detail = githubApiClient.getCommit(token, repository.getOwnerLogin(),
                    repository.getName(), commit.sha());
            int additions = detail.stats() == null ? 0 : detail.stats().additions();
            int deletions = detail.stats() == null ? 0 : detail.stats().deletions();
            int changedFiles = detail.files() == null ? 0 : detail.files().size();
            String title = commit.commit().message() == null ? null : commit.commit().message().lines().findFirst().orElse("");
            target.add(new CollectedActivity(commit.sha(), ActivityType.COMMIT, subjectGithubId,
                    commit.commit().author().date(), title, "COMMITTED", additions, deletions,
                    changedFiles, json(commit)));
        }
    }

    private void collectPullRequests(List<GitHubPullRequestDto> pulls, AnalysisJob job,
            long subjectGithubId, List<CollectedActivity> target) {
        for (GitHubPullRequestDto pull : pulls) {
            if (!isSubject(pull.user(), subjectGithubId) || !inside(pull.createdAt(), job)) {
                continue;
            }
            String state = pull.mergedAt() == null ? pull.state().toUpperCase() : "MERGED";
            target.add(new CollectedActivity(Long.toString(pull.id()), ActivityType.PULL_REQUEST,
                    subjectGithubId, pull.createdAt(), pull.title(), state, 0, 0, 0, json(pull)));
        }
    }

    private void collectReviews(String token, GitHubRepository repository, List<GitHubPullRequestDto> pulls,
            AnalysisJob job, long subjectGithubId, List<CollectedActivity> target) {
        for (GitHubPullRequestDto pull : pulls) {
            for (GitHubReviewDto review : githubApiClient.getReviews(token, repository.getOwnerLogin(),
                    repository.getName(), pull.number())) {
                if (!isSubject(review.user(), subjectGithubId) || !inside(review.submittedAt(), job)) {
                    continue;
                }
                target.add(new CollectedActivity(Long.toString(review.id()), ActivityType.REVIEW,
                        subjectGithubId, review.submittedAt(), "Review on #" + pull.number(),
                        review.state(), 0, 0, 0, json(review)));
            }
        }
    }

    private boolean isSubject(GitHubUserDto user, long subjectGithubId) {
        return user != null && user.id() == subjectGithubId && !user.isBot();
    }

    private boolean inside(Instant value, AnalysisJob job) {
        return value != null && !value.isBefore(job.getPeriodStart()) && value.isBefore(job.getPeriodEnd());
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize collected GitHub data", exception);
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return "0x" + java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
