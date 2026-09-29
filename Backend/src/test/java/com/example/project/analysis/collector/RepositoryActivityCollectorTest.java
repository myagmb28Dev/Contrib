package com.example.project.analysis.collector;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.example.project.analysis.benchmark.ActivityVector;
import com.example.project.github.client.GitHubApiClient;
import com.example.project.github.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;

class RepositoryActivityCollectorTest {
    @Test void filtersBotsSelfReviewsDuplicatesAndEndBoundaryWithoutFutureMergeLeakage() {
        var api = mock(GitHubApiClient.class);
        var mapper = new ObjectMapper().findAndRegisterModules();
        var collector = new RepositoryActivityCollector(api, mapper);
        var start = Instant.parse("2026-08-01T00:00:00Z");
        var end = Instant.parse("2026-09-01T00:00:00Z");
        var human = new GitHubUserDto(1, "robotics-student", "User");
        var reviewer = new GitHubUserDto(2, "reviewer", "User");
        var bot = new GitHubUserDto(3, "automation", "Bot");
        when(api.getRepository("token", "owner", "demo")).thenReturn(new GitHubRepositoryDto(1, human,
                "demo", "owner/demo", "https://github.com/owner/demo", false, "main", "C#", false));
        var commit = new GitHubCommitDto("sha", human, new GitHubCommitDto.CommitData(new GitHubCommitDto.CommitAuthor(start), "commit"), List.of(new GitHubCommitDto.Parent("parent")));
        var endCommit = new GitHubCommitDto("end", human, new GitHubCommitDto.CommitData(new GitHubCommitDto.CommitAuthor(end), "end"), List.of());
        var botCommit = new GitHubCommitDto("bot", bot, commit.commit(), List.of());
        when(api.getCommits(anyString(), anyString(), anyString(), anyString(), any(), any())).thenReturn(List.of(commit, commit, endCommit, botCommit));
        var pull = new GitHubPullRequestDto(10, 1, "PR", "closed", human, start, end.plusSeconds(1));
        when(api.getPullRequestsCreatedInPeriod(anyString(), anyString(), anyString(), any(), any())).thenReturn(List.of(pull, pull));
        var review = new GitHubReviewDto(20, "APPROVED", reviewer, start.plusSeconds(1));
        when(api.getReviews("token", "owner", "demo", 1)).thenReturn(List.of(review, review,
                new GitHubReviewDto(21, "APPROVED", human, start), new GitHubReviewDto(22, "APPROVED", bot, start),
                new GitHubReviewDto(23, "PENDING", reviewer, start), new GitHubReviewDto(24, "APPROVED", reviewer, end)));
        var result = collector.collect("token", "owner", "demo", "main", start, end, null);
        assertThat(result.activities()).hasSize(3);
        assertThat(result.activities()).filteredOn(a -> a.externalId().equals("10")).singleElement()
                .satisfies(a -> assertThat(a.state()).isEqualTo("CREATED"));
        assertThat(ActivityVector.from(result.activities())).isEqualTo(new ActivityVector(1, 1, 1, 1));
        verify(api, times(1)).getReviews("token", "owner", "demo", 1);
        verify(api, never()).getCommit(anyString(), anyString(), anyString(), anyString());
        assertThat(new GitHubUserDto(4, "renovate[bot]").isBot()).isTrue();
        assertThat(human.isBot()).isFalse();
    }
}
