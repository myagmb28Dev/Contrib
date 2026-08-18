package com.example.project.github.client;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.example.project.github.dto.GitHubCommitDetailDto;
import com.example.project.github.dto.GitHubCommitDto;
import com.example.project.github.dto.GitHubPullRequestDto;
import com.example.project.github.dto.GitHubRepositoryDto;
import com.example.project.github.dto.GitHubReviewDto;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GitHubApiClient {

    private static final int PAGE_SIZE = 100;
    private final RestClient restClient;

    public GitHubApiClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
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
        List<T> body = restClient.get().uri(uri).headers(headers -> bearer(headers, token))
                .exchange((request, response) -> {
                    ensureSuccess(response.getStatusCode(), response.getHeaders());
                    List<T> decoded = response.bodyTo(responseType);
                    return decoded == null ? List.of() : decoded;
                });
        return body == null ? List.of() : body;
    }

    private <T> T get(String token, URI uri, Class<T> responseType) {
        return restClient.get().uri(uri).headers(headers -> bearer(headers, token))
                .exchange((request, response) -> {
                    ensureSuccess(response.getStatusCode(), response.getHeaders());
                    return response.bodyTo(responseType);
                });
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
        if (status.value() == 403 || status.value() == 429) {
            throw new GitHubApiException(status.value(),
                    "GitHub API limit reached (remaining=%s, reset=%s)".formatted(remaining, reset));
        }
        throw new GitHubApiException(status.value(), "GitHub API request failed with status " + status.value());
    }

    @FunctionalInterface
    private interface PageUriFactory {
        URI create(int page);
    }
}
