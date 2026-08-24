package com.example.project.github.client;

import java.net.URI;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.example.project.github.dto.GitHubCommitDetailDto;
import com.example.project.github.dto.GitHubCommitDto;
import com.example.project.github.dto.GitHubPullRequestDto;
import com.example.project.github.dto.GitHubRepositoryDto;
import com.example.project.github.dto.GitHubReviewDto;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GitHubApiClient {

    private static final int PAGE_SIZE = 100;
    private final RestClient restClient;
    private final int maxAttempts;
    private final Duration baseBackoff;
    private final Duration maxBackoff;
    private final Sleeper sleeper;

    @Autowired
    public GitHubApiClient(RestClient.Builder builder,
            @Value("${app.github.rate-limit.max-attempts:4}") int maxAttempts,
            @Value("${app.github.rate-limit.base-backoff:PT1S}") Duration baseBackoff,
            @Value("${app.github.rate-limit.max-backoff:PT30S}") Duration maxBackoff) {
        this(builder, maxAttempts, baseBackoff, maxBackoff, Thread::sleep);
    }

    GitHubApiClient(RestClient.Builder builder, int maxAttempts, Duration baseBackoff,
            Duration maxBackoff, Sleeper sleeper) {
        this.restClient = builder.baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
        this.maxAttempts = Math.max(1, maxAttempts);
        this.baseBackoff = baseBackoff;
        this.maxBackoff = maxBackoff;
        this.sleeper = sleeper;
    }

    public List<GitHubRepositoryDto> getPublicRepositories(String token) {
        return getAllPages(token,
                page -> URI.create("https://api.github.com/user/repos?visibility=public&affiliation=owner,collaborator&sort=full_name&per_page="
                        + PAGE_SIZE + "&page=" + page),
                new ParameterizedTypeReference<>() {});
    }

    public List<GitHubCommitDto> getCommits(String token, String owner, String repository,
            String branch, Instant since, Instant until) {
        return getAllPages(token,
                page -> UriComponentsBuilder.fromUriString("https://api.github.com/repos/{owner}/{repository}/commits")
                        .queryParam("sha", branch)
                        .queryParam("since", since)
                        .queryParam("until", until)
                        .queryParam("per_page", PAGE_SIZE)
                        .queryParam("page", page)
                        .buildAndExpand(owner, repository).encode().toUri(),
                new ParameterizedTypeReference<>() {});
    }

    public GitHubCommitDetailDto getCommit(String token, String owner, String repository, String sha) {
        return get(token, URI.create("https://api.github.com/repos/%s/%s/commits/%s".formatted(owner, repository, sha)),
                GitHubCommitDetailDto.class);
    }

    public List<GitHubPullRequestDto> getPullRequests(String token, String owner, String repository) {
        return getAllPages(token,
                page -> URI.create("https://api.github.com/repos/%s/%s/pulls?state=all&sort=created&direction=desc&per_page=%d&page=%d"
                        .formatted(owner, repository, PAGE_SIZE, page)),
                new ParameterizedTypeReference<>() {});
    }

    public List<GitHubReviewDto> getReviews(String token, String owner, String repository, int pullNumber) {
        return getAllPages(token,
                page -> URI.create("https://api.github.com/repos/%s/%s/pulls/%d/reviews?per_page=%d&page=%d"
                        .formatted(owner, repository, pullNumber, PAGE_SIZE, page)),
                new ParameterizedTypeReference<>() {});
    }

    private <T> List<T> getAllPages(String token, PageUriFactory uriFactory,
            ParameterizedTypeReference<List<T>> responseType) {
        List<T> result = new ArrayList<>();
        for (int page = 1; page <= 100; page++) {
            List<T> items = exchangeList(token, uriFactory.create(page), responseType);
            result.addAll(items);
            if (items.size() < PAGE_SIZE) {
                break;
            }
        }
        return result;
    }

    private <T> List<T> exchangeList(String token, URI uri, ParameterizedTypeReference<List<T>> responseType) {
        List<T> body = withRateLimitRetry(() -> restClient.get().uri(uri).headers(headers -> bearer(headers, token))
                .exchange((request, response) -> {
                    ensureSuccess(response.getStatusCode(), response.getHeaders());
                    List<T> decoded = response.bodyTo(responseType);
                    return decoded == null ? List.of() : decoded;
                }));
        return body == null ? List.of() : body;
    }

    private <T> T get(String token, URI uri, Class<T> responseType) {
        return withRateLimitRetry(() -> restClient.get().uri(uri).headers(headers -> bearer(headers, token))
                .exchange((request, response) -> {
                    ensureSuccess(response.getStatusCode(), response.getHeaders());
                    return response.bodyTo(responseType);
                }));
    }

    private void bearer(HttpHeaders headers, String token) {
        headers.setBearerAuth(token);
    }

    private void ensureSuccess(HttpStatusCode status, HttpHeaders headers) {
        if (status.is2xxSuccessful()) {
            return;
        }
        String remaining = headers.getFirst("X-RateLimit-Remaining");
        String reset = headers.getFirst("X-RateLimit-Reset");
        boolean rateLimited = status.value() == 429 || status.value() == 403
                && ("0".equals(remaining) || reset != null || headers.getFirst(HttpHeaders.RETRY_AFTER) != null);
        if (rateLimited) {
            Instant retryAt = retryAt(headers);
            throw new GitHubApiException(status.value(),
                    "GitHub API limit reached (remaining=%s, reset=%s)".formatted(remaining, reset), retryAt);
        }
        throw new GitHubApiException(status.value(), "GitHub API request failed with status " + status.value());
    }

    private Instant retryAt(HttpHeaders headers) {
        String retryAfter = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (retryAfter != null) {
            try {
                return Instant.now().plusSeconds(Long.parseLong(retryAfter));
            } catch (NumberFormatException ignored) {
                // Fall through to the GitHub reset header.
            }
        }
        String reset = headers.getFirst("X-RateLimit-Reset");
        if (reset != null) {
            try {
                return Instant.ofEpochSecond(Long.parseLong(reset));
            } catch (NumberFormatException ignored) {
                // Use exponential backoff when GitHub sends a malformed header.
            }
        }
        return null;
    }

    private <T> T withRateLimitRetry(Supplier<T> request) {
        GitHubApiException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return request.get();
            } catch (GitHubApiException exception) {
                last = exception;
                if (!exception.isRateLimited() || attempt == maxAttempts) {
                    throw exception;
                }
                sleep(backoff(exception, attempt));
            }
        }
        throw last;
    }

    private Duration backoff(GitHubApiException exception, int attempt) {
        Duration exponential = baseBackoff.multipliedBy(1L << Math.min(attempt - 1, 20));
        Duration requested = exception.getRetryAt() == null
                ? exponential
                : Duration.between(Instant.now(), exception.getRetryAt());
        if (requested.isNegative()) {
            requested = Duration.ZERO;
        }
        return requested.compareTo(maxBackoff) > 0 ? maxBackoff : requested;
    }

    private void sleep(Duration duration) {
        try {
            sleeper.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GitHubApiException(429, "Interrupted while waiting for the GitHub API rate limit");
        }
    }

    @FunctionalInterface
    private interface PageUriFactory {
        URI create(int page);
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }
}
