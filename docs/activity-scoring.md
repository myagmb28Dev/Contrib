# Activity scoring and comparison model

## Meaning and compatibility

The existing `score-v1` is an **experimental activity score**, not a measure of developer ability, quality, or productivity. Its capped arithmetic is unchanged: commits 25, PR creation/merge 30, reviews 20, active days 15, changed-file occurrences 10 points maximum.

New analyses add immutable `activityComparison` JSON. Certificates containing it use schema 1.1. Legacy analyses and certificates remain unchanged; certificates created from legacy analyses retain schema 1.0. Hash verification certifies payload integrity, not fairness or external endorsement of the score.

## github-v2 collection contract

- Exact UTC `[start,end)`. UI end dates are inclusive calendar dates and are submitted as midnight on the following day.
- Non-merge selected-branch commits returned by GitHub's since/until endpoint, additionally filtered by author timestamp. Merge strategy and author attribution affect observed counts.
- PRs created in the interval. Merged PRs are the subset also merged before the exclusive end; future merges do not inflate past periods.
- Reviews submitted in the interval **on PRs created in that interval only**. Pending and self reviews are excluded. Reviews on older PRs are outside the model. API state is observed at collection time, not a reconstruction of deleted history.
- Exclude GitHub Bot account type and `[bot]` login suffix, not arbitrary human names containing `bot`. Unknown/deleted authors are excluded. Automation using human accounts remains a limitation.
- Deduplicate type/external ID. Count distinct active UTC dates. Missing API responses, rate-limit failures and pagination-cap exhaustion fail collection instead of producing unmarked partial scores.
- Collect repository-wide activity size, but persist only the subject's raw events in their personal snapshot. Public benchmark exports contain numeric aggregates and event fingerprints, not tokens, emails, titles, bodies or private repository data.

## activity-percentile-v1

1. Match exact period, primary language, and active-contributor band: 1-9, 10-49, or 50+. Only public, non-fork, non-archived default branches qualify. These are coarse environmental proxies, not controls for developer role or team workflow.
2. Exclude the subject's GitHub ID from every reference repository. Require at least **3 represented repositories and 20 distinct remaining contributors**. Do not silently broaden the cohort.
3. Observation unit: contributor-repository pair. A person present in k matched repositories gives each observation weight 1/k, so their total influence is 1. Report both observation count and distinct-person count.
4. Candidate dimensions: commits, created PRs, reviews, active days. Retain dimensions with positive activity from at least `max(5, ceil(20% of unique people))` people. Require at least two supported dimensions. Apply equal weights to supported dimensions and display them. Merged PRs and changed files are supplementary, with no extra percentile bonus.
5. Dimension rank: `100 * (weight below + 0.5 * weight tied) / total weight`. Average the unrounded dimension ranks into a composite. The **final percentile** is the weighted midrank of that composite among identically calculated reference composites. An average of percentiles alone is not labelled as a percentile.
6. Display one decimal. 100 means above all reference composites, not perfect ability. Ties get the middle rank. Different cohorts/periods are not directly comparable; list sorting stays explicitly based on the legacy activity score.
7. No observed activity gives NO_ACTIVITY. Other unavailable states: UNSUPPORTED_SCOPE, NO_REFERENCE_PERIOD, INSUFFICIENT_COHORT, INSUFFICIENT_DIMENSIONS. No numeric percentile is fabricated in these states.
8. Compute local sensitivity to adding one commit, PR or review while holding other dimensions fixed. Changes of at least 20 percentile points show a prominent warning. This diagnostic is not a confidence interval; sparse samples and ties can cause large jumps.

## Public reference data

Initial bundled coverage is C# repositories for August 2026 and June-August 2026. This is a curated convenience sample, not a representative population. Other languages, dates or missing size bands deliberately show an unavailable comparison. The analysis form lists published reference periods.

Export explicitly, offline, with `GH_TOKEN` in the process environment:

```powershell
cd Backend
.\gradlew.bat exportBenchmark '-PbenchmarkArgs=2026-06-01T00:00:00Z,2026-09-01T00:00:00Z,src/main/resources/benchmarks/new.json,owner/repo,owner/another'
.\gradlew.bat -q validateBenchmark '-PbenchmarkArgs=src/main/resources/benchmarks/new.json'
```

The exporter rejects open/future periods, private/fork/archived repositories, and file overwrites. The catalog rejects duplicate periods/repositories/people, wrong schema/collector versions, negative counts and impossible active-day counts. Review sample selection before publication.

Dataset ID is SHA-256 of UTF-8 JSON with LF line endings. `/api/public/benchmarks/{id}` returns exactly these normalized bytes. Preserve published files permanently. Adding more languages to an existing period requires explicit catalog versioning rather than modifying already referenced snapshots. Model version, dataset and cohort IDs, period, repository list, population counts, weights and rules are frozen per result. Job deduplication uses a pipeline version containing a digest of the model and reference dataset (or `none`), so publishing a previously missing reference permits a new analysis without rewriting an old result. Identical period/branch/model/reference requests return the original snapshot. The source metadata records the underlying collection version separately.

See [real-data validation](activity-benchmark-validation.md) for distributions and perturbation checks. Passing software tests does not establish empirical validity as a measure of developer value.

## References

- [CHAOSS metrics](https://handbook.chaoss.community/community-handbook/community-initiatives/metrics): community activity measurement, not approval of our weights or individual rankings.
- [SPACE](https://www.microsoft.com/en-us/research/publication/the-space-of-developer-productivity-theres-more-to-it-than-you-think/): productivity cannot be reduced to a single activity dimension.
- [GitHub PR API](https://docs.github.com/en/rest/pulls/pulls): creation-order pagination and event fields.
