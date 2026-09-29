package com.example.project.analysis.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import com.example.project.auth.service.GitHubAccessTokenService;
import com.example.project.github.client.GitHubApiClient;
import com.example.project.github.dto.GitHubRepositoryDto;
import com.example.project.github.dto.GitHubUserDto;
import com.example.project.repository.domain.GitHubRepository;
import com.example.project.analysis.dto.CreateAnalysisRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AnalysisPeriodResolverTest {
    private static final Instant CREATED = Instant.parse("2011-04-12T13:45:27Z");
    private static final Instant NOW = Instant.parse("2026-09-29T02:13:44.123Z");
    private final GitHubApiClient api = mock(GitHubApiClient.class);
    private final GitHubAccessTokenService tokens = mock(GitHubAccessTokenService.class);
    private final GitHubRepository repo = mock(GitHubRepository.class);
    private final UUID userId = UUID.randomUUID();
    private final AnalysisPeriodResolver resolver = new AnalysisPeriodResolver(api, tokens, Clock.fixed(NOW, ZoneOffset.UTC));

    private void repository(Instant creationTime) {
        when(repo.getGithubRepositoryId()).thenReturn(42L);
        when(repo.getOwnerLogin()).thenReturn("owner");
        when(repo.getName()).thenReturn("demo");
        when(tokens.getValidAccessToken(userId)).thenReturn("token");
        when(api.getRepository("token", "owner", "demo")).thenReturn(new GitHubRepositoryDto(42,
                new GitHubUserDto(1, "owner"), "demo", "owner/demo", "https://github.com/owner/demo", false,
                "develop", "Java", false, false, creationTime));
    }

    @Test void allTimeUsesExactGitHubCreationAndServerNowInsteadOfLocalDatesOrYearCaps() {
        repository(CREATED);
        when(repo.getGithubCreatedAt()).thenReturn(NOW.minusSeconds(86400));
        var result = resolver.resolve(userId, repo, NOW.minusSeconds(3600), NOW.plusSeconds(86400), true);
        assertThat(result.start()).isEqualTo(CREATED);
        assertThat(result.end()).isEqualTo(NOW);
        verify(repo).updateGithubCreatedAt(CREATED);
    }

    @Test void missingOrFutureCreationTimeFailsInsteadOfInventingOneYear() {
        repository(null);
        assertThatThrownBy(() -> resolver.resolve(userId, repo, null, null, true))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("생성 시각");
        repository(NOW.plusSeconds(1));
        assertThatThrownBy(() -> resolver.resolve(userId, repo, null, null, true)).isInstanceOf(IllegalStateException.class);
    }

    @Test void serverNowUsesDatabasePrecisionSoReturnedAndCollectedPeriodsStayIdentical() {
        repository(CREATED);
        var preciseClock = Clock.fixed(NOW.plusNanos(987), ZoneOffset.UTC);
        var result = new AnalysisPeriodResolver(api, tokens, preciseClock).resolve(userId, repo, null, null, true);
        assertThat(result.end()).isEqualTo(NOW);
    }

    @Test void explicitWindowIsPreservedAndDoesNotFetchCreationMetadata() {
        var result = resolver.resolve(userId, repo, CREATED, NOW, false);
        assertThat(result).isEqualTo(new AnalysisPeriodResolver.Period(CREATED, NOW));
        verifyNoInteractions(api, tokens);
        assertThatThrownBy(() -> resolver.resolve(userId, repo, NOW, CREATED, false)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void jsonReadsGitHubCreatedAtAndFullHistoryRequestDoesNotRequireClientDates() throws Exception {
        var mapper = new ObjectMapper().findAndRegisterModules();
        var metadata = mapper.readValue("{\"id\":42,\"created_at\":\"2011-04-12T13:45:27Z\"}", GitHubRepositoryDto.class);
        assertThat(metadata.createdAt()).isEqualTo(CREATED);
        var request = mapper.readValue("{\"allTime\":true}", CreateAnalysisRequest.class);
        assertThat(request.isPeriodValid()).isTrue();
        assertThat(new CreateAnalysisRequest(null, NOW).isPeriodValid()).isFalse();
        assertThat(new CreateAnalysisRequest(NOW, CREATED).isPeriodValid()).isFalse();
    }
}
