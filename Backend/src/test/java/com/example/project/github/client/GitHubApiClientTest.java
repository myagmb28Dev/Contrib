package com.example.project.github.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withTooManyRequests;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withForbiddenRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GitHubApiClientTest {

    @Test
    void failsRatherThanReturningAnUnmarkedTruncatedCollection() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GitHubApiClient client = new GitHubApiClient(builder, 1, Duration.ZERO, Duration.ZERO, millis -> {});
        String fullPage = "[" + String.join(",", java.util.Collections.nCopies(100, "{\"id\":1}")) + "]";
        for (int page = 1; page <= 100; page++) {
            server.expect(requestTo("https://api.github.com/user/repos?visibility=public&affiliation=owner,collaborator&sort=full_name&per_page=100&page=" + page))
                    .andRespond(withSuccess(fullPage, MediaType.APPLICATION_JSON));
        }
        assertThatThrownBy(() -> client.getPublicRepositories("token"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("incomplete");
        server.verify();
    }

    @Test
    void filtersPullCreationWindowIncludingStartExcludingEnd() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GitHubApiClient client = new GitHubApiClient(builder, 1, Duration.ZERO, Duration.ZERO, millis -> {});
        server.expect(requestTo("https://api.github.com/repos/owner/demo/pulls?state=all&sort=created&direction=desc&per_page=100&page=1"))
                .andRespond(withSuccess("""
                        [{"id":3,"created_at":"2026-09-01T00:00:00Z"},
                         {"id":2,"created_at":"2026-08-01T00:00:00Z"},
                         {"id":1,"created_at":"2026-07-31T23:59:59Z"}]
                        """, MediaType.APPLICATION_JSON));
        assertThat(client.getPullRequestsCreatedInPeriod("token", "owner", "demo",
                java.time.Instant.parse("2026-08-01T00:00:00Z"), java.time.Instant.parse("2026-09-01T00:00:00Z")))
                .extracting(p -> p.id()).containsExactly(2L);
        server.verify();
    }

    @Test
    void fetchesPrivateRepositoriesWithAllVisibility() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GitHubApiClient client = new GitHubApiClient(builder, 1, Duration.ZERO,
                Duration.ZERO, millis -> { });
        server.expect(requestTo("https://api.github.com/user/repos?visibility=all&affiliation=owner,collaborator&sort=full_name&per_page=100&page=1"))
                .andRespond(withSuccess("[{\"id\":1,\"private\":true}]", MediaType.APPLICATION_JSON));
        assertThat(client.getAccessibleRepositories("token")).singleElement()
                .satisfies(repo -> assertThat(repo.privateRepository()).isTrue());
        server.verify();
    }

    @Test
    void retriesRateLimitedRequestAndKeepsPaginationResult() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        List<Long> sleeps = new ArrayList<>();
        GitHubApiClient client = new GitHubApiClient(builder, 3, Duration.ofMillis(25),
                Duration.ofSeconds(1), sleeps::add);
        String url = "https://api.github.com/user/repos?visibility=public&affiliation=owner,collaborator&sort=full_name&per_page=100&page=1";

        server.expect(once(), requestTo(url))
                .andRespond(withTooManyRequests().header("Retry-After", "0"));
        server.expect(once(), requestTo(url))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.getPublicRepositories("token")).isEmpty();
        assertThat(sleeps).containsExactly(0L);
        server.verify();
    }

    @Test
    void stopsAfterConfiguredRateLimitAttempts() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GitHubApiClient client = new GitHubApiClient(builder, 2, Duration.ZERO,
                Duration.ZERO, millis -> { });
        String url = "https://api.github.com/user/repos?visibility=public&affiliation=owner,collaborator&sort=full_name&per_page=100&page=1";

        server.expect(once(), requestTo(url)).andRespond(withTooManyRequests());
        server.expect(once(), requestTo(url)).andRespond(withTooManyRequests());

        assertThatThrownBy(() -> client.getPublicRepositories("token"))
                .isInstanceOf(GitHubApiException.class)
                .satisfies(error -> assertThat(((GitHubApiException) error).getStatusCode()).isEqualTo(429));
        server.verify();
    }

    @Test
    void doesNotRetryOrdinaryForbiddenResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        List<Long> sleeps = new ArrayList<>();
        GitHubApiClient client = new GitHubApiClient(builder, 3, Duration.ZERO,
                Duration.ZERO, sleeps::add);
        String url = "https://api.github.com/user/repos?visibility=public&affiliation=owner,collaborator&sort=full_name&per_page=100&page=1";
        server.expect(once(), requestTo(url)).andRespond(withForbiddenRequest());

        assertThatThrownBy(() -> client.getPublicRepositories("token"))
                .isInstanceOf(GitHubApiException.class)
                .satisfies(error -> assertThat(((GitHubApiException) error).isRateLimited()).isFalse());
        assertThat(sleeps).isEmpty();
        server.verify();
    }
}
