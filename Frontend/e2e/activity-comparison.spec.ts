import { expect, test, type Page } from "@playwright/test";

const comparison = {
  status: "AVAILABLE", modelVersion: "activity-percentile-v1", percentile: 80,
  metricPercentiles: { commits: 75, pullRequestsOpened: 85, reviews: 70, activeDays: 90 },
  metricWeights: { commits: 0.25, pullRequestsOpened: 0.25, reviews: 0.25, activeDays: 0.25 },
  datasetId: "0xreference", cohortId: "0xcohort", referenceCollectedAt: "2026-09-02T00:00:00Z",
  periodStart: "2026-08-01T00:00:00Z", periodEnd: "2026-09-01T00:00:00Z", language: "C#",
  sizeBand: "10-49", sampleCount: 32, contributorCount: 30,
  repositories: ["owner/one", "owner/two", "owner/three"], scope: "UTC period", calculationRules: "weighted midrank",
  reason: "선택된 비교 집단의 활동 수준입니다.", singleActivitySensitivity: 30,
};

test("isolated preview shows only a clearly labelled percentile sample", async ({ page }) => {
  await page.route("**/api/**", route => route.fulfill({ status: 401, json: { message: "preview has no session" } }));
  await page.goto("/preview/percentile");
  await expect(page.getByRole("heading", { name: "실험적 백분위 미리보기", exact: true })).toBeVisible();
  await expect(page.locator(".comparison-percentile")).toHaveText("80.0");
  await expect(page.getByText("미리보기용 샘플입니다. 실제 GitHub 기여 분석 결과가 아닙니다.", { exact: true })).toBeVisible();
  await expect(page.getByText("실험적 활동 점수 v1", { exact: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "기여 인증서 발급하기" })).toHaveCount(0);
});


async function fixtures(page: Page, result: unknown, certificateScore: number | null = 0) {
  await page.route("**/api/**", async route => {
    const path = new URL(route.request().url()).pathname;
    const json = (value: unknown, status = 200) => route.fulfill({ status, contentType: "application/json", body: JSON.stringify(value) });
    if (path === "/api/auth/me") return json({ userId: "user", githubUserId: 1, githubUsername: "person", email: null });
    if (path === "/api/auth/csrf") return json({ headerName: "X-XSRF-TOKEN", token: "test" });
    if (path === "/api/analyses/analysis") return json({ id: "analysis", jobId: "job", repositoryId: "repo",
      periodStart: comparison.periodStart, periodEnd: comparison.periodEnd,
      metrics: { commits: 4, pullRequestsOpened: 2, reviews: 1, activeDays: 3 }, score: 42,
      scoreVersion: "score-v1", calculationRules: "v1", technicalAreas: [], summary: null, activityComparison: result });
    if (path.endsWith("/verification")) return json({ status: "NOT_REGISTERED", publicId: "public", storedHash: "0xabc", calculatedHash: "0xabc" });
    if (path.endsWith("/attestation")) return json({}, 404);
    if (path.includes("/certificates/")) return json({ id: "certificate", publicId: "public", analysisId: "analysis",
      schemaVersion: "1.1", payload: { repository: { fullName: "owner/demo" }, result: {
        ...(certificateScore === null ? {} : { score: certificateScore }), activityComparison: result,
      } }, hash: "0xabc", status: "ISSUED", issuedAt: "2026-09-02T00:00:00Z", subjectWalletAddress: null });
    return json({}, 404);
  });
}

test("explains percentile, cohort, sensitivity and reference data without the legacy activity score", async ({ page }) => {
  await fixtures(page, comparison);
  await page.goto("/repositories/repo/analysis/analysis");
  const card = page.getByRole("region", { name: "상대 활동 수준", exact: true });
  await expect(card.locator(".comparison-percentile")).toHaveText("80.0");
  await expect(card).toContainText("30명");
  await expect(card.getByRole("note")).toContainText("30.0백분위");
  await card.getByText("산정 기준과 비교 데이터 확인", { exact: true }).click();
  await expect(card.getByRole("link", { name: /사용된 공개 비교 데이터/ })).toHaveAttribute("href", "/api/public/benchmarks/0xreference");
  await expect(page.getByText("실험적 활동 점수 v1", { exact: true })).toHaveCount(0);
  await expect(page.locator(".analysis-score-block")).toHaveCount(0);
  await expect(page.getByText("Excellent Contribution", { exact: true })).toHaveCount(0);
});

test("insufficient reference data has a reason and no fabricated percentile", async ({ page }) => {
  await fixtures(page, { ...comparison, status: "INSUFFICIENT_COHORT", percentile: null, reason: "최소 20명의 기여자가 필요합니다." });
  await page.goto("/repositories/repo/analysis/analysis");
  await expect(page.getByText("백분위 산정 보류", { exact: true })).toBeVisible();
  await expect(page.getByText("최소 20명의 기여자가 필요합니다.", { exact: true })).toBeVisible();
  await expect(page.locator(".comparison-percentile")).toHaveCount(0);
});

test("nested certificate payload shows only the frozen comparison publicly", async ({ page }) => {
  await fixtures(page, comparison, 0);
  await page.goto("/certificates/certificate");
  await expect(page.locator(".preview-score")).toHaveCount(0);
  await expect(page.locator(".certificate-repository strong")).toHaveText("owner/demo");
  await expect(page.getByRole("region", { name: "인증서 미리보기" })).toBeVisible();
  await expect(page.locator(".certificate-document-percentile strong")).toHaveText("80.0");
  await expect(page.getByRole("region", { name: "상대 활동 수준" })).toBeHidden();
  await page.getByText("산정 근거 확인", { exact: true }).click();
  await expect(page.getByRole("region", { name: "상대 활동 수준" })).toBeVisible();
  await page.getByRole("link", { name: "공개 검증 화면 열기", exact: true }).click();
  await expect(page.locator(".comparison-percentile")).toHaveText("80.0");
});

test("legacy certificate with no recorded score never falls back to 80", async ({ page }) => {
  await fixtures(page, null, null);
  await page.goto("/certificates/certificate");
  await expect(page.locator(".preview-score")).toHaveCount(0);
  await expect(page.locator(".certificate-document-percentile")).toHaveCount(0);
  await page.getByText("산정 근거 확인", { exact: true }).click();
  await expect(page.getByText(/이전 분석에는 비교 데이터가 없습니다/)).toBeVisible();
  await expect(page.locator(".comparison-percentile")).toHaveCount(0);
});

test("published reference periods do not change the full-history-only form", async ({ page }) => {
  await fixtures(page, comparison);
  await page.route("**/api/public/benchmarks", route => route.fulfill({ json: [{ id: "summer", periodStart: "2026-06-01T00:00:00Z",
    periodEnd: "2026-09-01T00:00:00Z", collectedAt: "2026-09-29T00:00:00Z", repositoryCount: 12, languages: ["C#"] }] }));
  await page.route("**/api/repositories/repo", route => route.fulfill({ json: { id: "repo", name: "demo", defaultBranch: "main" } }));
  await page.route("**/api/repositories/repo/branches", route => route.fulfill({ json: ["main"] }));
  let submitted: Record<string, unknown> | null = null;
  await page.route("**/api/repositories/repo/analyses", route => {
    if (route.request().method() !== "POST") return route.fulfill({ json: [] });
    submitted = route.request().postDataJSON();
    return route.fulfill({ json: { id: "job", status: "COMPLETED", progress: 100, periodStart: "2011-04-12T13:45:27Z", periodEnd: "2026-09-29T02:00:00Z" } });
  });
  await page.goto("/repositories/repo/analyze");
  await expect(page.getByRole("combobox")).toHaveCount(0);
  await expect(page.locator('input[type="date"]')).toHaveCount(0);
  await page.getByRole("button", { name: "기여 분석 시작하기", exact: true }).click();
  await expect.poll(() => submitted).toEqual({ allTime: true });
});
