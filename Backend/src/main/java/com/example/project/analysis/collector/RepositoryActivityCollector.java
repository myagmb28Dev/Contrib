package com.example.project.analysis.collector;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.project.analysis.domain.ActivityType;
import com.example.project.github.client.GitHubApiClient;
import com.example.project.github.dto.GitHubRepositoryDto;
import com.example.project.github.dto.GitHubUserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/** Shared collection semantics for user analyses and offline public benchmark exports. */
@Component
public class RepositoryActivityCollector {
    public static final String VERSION = "github-v2";
    public static final String SCOPE = "Non-merge branch commits by author timestamp; PRs created in [start,end); "
            + "merged is a subset of those PRs merged before end; submitted non-self reviews on those PRs only; "
            + "UTC days; GitHub Bot type and [bot] suffix excluded; unique type/externalId; unknown authors excluded";
    private final GitHubApiClient api;
    private final ObjectMapper mapper;

    public RepositoryActivityCollector(GitHubApiClient api, ObjectMapper mapper) {
        this.api = api;
        this.mapper = mapper;
    }

    public record Collection(GitHubRepositoryDto repository, String branch, List<CollectedActivity> activities) {}

    public Collection collect(String token, String owner, String name, String branch,
            Instant start, Instant end, Long detailedSubject) {
        if (!start.isBefore(end)) throw new IllegalArgumentException("Invalid collection period");
        var repository = api.getRepository(token, owner, name);
        if (repository == null) throw new IllegalStateException("GitHub repository metadata is missing");
        String selectedBranch = branch == null || branch.isBlank() ? repository.defaultBranch() : branch;
        Map<String, CollectedActivity> events = new LinkedHashMap<>();
        for (var commit : api.getCommits(token, owner, name, selectedBranch, start, end)) {
            if (!human(commit.author()) || commit.parents() == null || commit.parents().size() > 1
                    || commit.commit() == null || commit.commit().author() == null
                    || !inside(commit.commit().author().date(), start, end)) continue;
            String key = "COMMIT:" + commit.sha();
            if (events.containsKey(key)) continue;
            int additions = 0, deletions = 0, files = 0;
            if (detailedSubject != null && commit.author().id() == detailedSubject) {
                var detail = api.getCommit(token, owner, name, commit.sha());
                if (detail == null) throw new IllegalStateException("GitHub commit detail is missing");
                additions = detail.stats() == null ? 0 : detail.stats().additions();
                deletions = detail.stats() == null ? 0 : detail.stats().deletions();
                files = detail.files() == null ? 0 : detail.files().size();
            }
            String title = commit.commit().message() == null ? "" : commit.commit().message().lines().findFirst().orElse("");
            events.put(key, new CollectedActivity(commit.sha(), ActivityType.COMMIT, commit.author().id(),
                    commit.commit().author().date(), title, "COMMITTED", additions, deletions, files, json(commit)));
        }
        var pulls = api.getPullRequestsCreatedInPeriod(token, owner, name, start, end);
        var seenPulls = new java.util.HashSet<Long>();
        for (var pull : pulls) {
            if (!inside(pull.createdAt(), start, end) || !seenPulls.add(pull.id())) continue;
            if (human(pull.user())) {
                String state = inside(pull.mergedAt(), start, end) ? "MERGED" : "CREATED";
                events.put("PULL_REQUEST:" + pull.id(), new CollectedActivity(Long.toString(pull.id()),
                        ActivityType.PULL_REQUEST, pull.user().id(), pull.createdAt(), pull.title(), state,
                        0, 0, 0, json(pull)));
            }
            for (var review : api.getReviews(token, owner, name, pull.number())) {
                if (!human(review.user()) || !inside(review.submittedAt(), start, end)
                        || "PENDING".equalsIgnoreCase(review.state())
                        || pull.user() != null && review.user().id() == pull.user().id()) continue;
                events.putIfAbsent("REVIEW:" + review.id(), new CollectedActivity(Long.toString(review.id()),
                        ActivityType.REVIEW, review.user().id(), review.submittedAt(), "Review on #" + pull.number(),
                        review.state(), 0, 0, 0, json(review)));
            }
        }
        var sorted = events.values().stream().sorted(Comparator.comparing(CollectedActivity::occurredAt)
                .thenComparing(event -> event.type().name()).thenComparing(CollectedActivity::externalId)).toList();
        return new Collection(repository, selectedBranch, sorted);
    }

    private static boolean human(GitHubUserDto user) {
        return user != null && user.id() > 0 && !user.isBot();
    }

    public static boolean inside(Instant time, Instant start, Instant end) {
        return time != null && !time.isBefore(start) && time.isBefore(end);
    }

    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception e) { throw new IllegalStateException("Cannot serialize collected activity", e); }
    }
}
