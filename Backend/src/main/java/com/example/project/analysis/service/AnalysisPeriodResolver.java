package com.example.project.analysis.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import com.example.project.auth.service.GitHubAccessTokenService;
import com.example.project.github.client.GitHubApiClient;
import com.example.project.repository.domain.GitHubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AnalysisPeriodResolver {
    private final GitHubApiClient api;
    private final GitHubAccessTokenService tokens;
    private final Clock clock;

    @Autowired
    public AnalysisPeriodResolver(GitHubApiClient api, GitHubAccessTokenService tokens) {
        this(api, tokens, Clock.systemUTC());
    }

    AnalysisPeriodResolver(GitHubApiClient api, GitHubAccessTokenService tokens, Clock clock) {
        this.api = api;
        this.tokens = tokens;
        this.clock = clock;
    }

    public record Period(Instant start, Instant end) {}

    public Period resolve(UUID userId, GitHubRepository repository, Instant requestedStart,
            Instant requestedEnd, boolean allTime) {
        if (!allTime) {
            if (requestedStart == null || requestedEnd == null || !requestedStart.isBefore(requestedEnd)) {
                throw new IllegalArgumentException("분석 시작 시각은 종료 시각보다 빨라야 합니다.");
            }
            return new Period(requestedStart, requestedEnd);
        }
        var source = api.getRepository(tokens.getValidAccessToken(userId), repository.getOwnerLogin(), repository.getName());
        if (source == null || source.id() != repository.getGithubRepositoryId() || source.createdAt() == null) {
            throw new IllegalStateException("GitHub 저장소 생성 시각을 확인할 수 없습니다. 동기화 후 다시 시도해주세요.");
        }
        // PostgreSQL/H2 persist timestamps at microsecond precision. Keep the response and worker window identical.
        Instant end = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        if (!source.createdAt().isBefore(end)) {
            throw new IllegalStateException("저장소 생성 시각 이후에 분석 가능한 기간이 없습니다.");
        }
        repository.updateGithubCreatedAt(source.createdAt());
        return new Period(source.createdAt(), end);
    }
}
