import { expect, test, type Page } from "@playwright/test";

async function setup(page: Page, githubCreatedAt: string | null) {
  let branchCalls = 0;
  const requests: Record<string, unknown>[] = [];
  await page.route("**/api/**", route => {
    const path = new URL(route.request().url()).pathname;
    const json = (body: unknown, status = 200) => route.fulfill({ status, json: body });
    if (path === "/api/auth/me") return json({ userId: "user", githubUserId: 1, githubUsername: "person", email: null });
    if (path === "/api/auth/csrf") return json({ headerName: "X-XSRF-TOKEN", token: "test" });
    if (path === "/api/public/benchmarks") return json([]);
    if (path === "/api/repositories/repo") return json({ id: "repo", name: "history", defaultBranch: "develop",
      createdAt: "2026-09-29T00:00:00Z", githubCreatedAt });
    if (path.endsWith("/branches")) { branchCalls++; return json(["develop", "main"]); }
    if (path === "/api/repositories/repo/analyses") {
      if (route.request().method() === "GET") return json([]);
      const body = route.request().postDataJSON();
      requests.push(body);
      return json({ id: "job", status: "COMPLETED", progress: 100,
        periodStart: body.allTime ? "2011-04-12T13:45:27Z" : body.periodStart,
        periodEnd: body.allTime ? "2026-09-29T02:13:44.123Z" : body.periodEnd });
    }
    return json({}, 404);
  });
  return { requests, branchCalls: () => branchCalls };
}

test("full history sends server-resolved mode, not a fixed number of years or a branch", async ({ page }) => {
  const state = await setup(page, "2011-04-12T13:45:27Z");
  await page.goto("/repositories/repo/analyze");
  await page.getByRole("button", { name: "전체 기간", exact: true }).click();
  await expect(page.getByLabel("시작일 (Start Date)")).toHaveValue("2011-04-12");
  await expect(page.getByRole("status")).toContainText("2011-04-12T13:45:27Z");
  await expect(page.getByLabel("분석 브랜치", { exact: true })).toHaveCount(0);
  await page.getByRole("button", { name: "기여 분석 시작하기", exact: true }).click();
  await expect.poll(() => state.requests).toEqual([{ allTime: true }]);
  await expect(page.getByText(/실제 분석 기간/)).toContainText("2011-04-12T13:45:27Z");
  await expect(page.getByLabel("종료일 (End Date)")).toHaveValue("2026-09-29");
  expect(state.branchCalls()).toBe(0);
});

test("legacy rows never substitute their local registration date for GitHub creation", async ({ page }) => {
  const state = await setup(page, null);
  await page.goto("/repositories/repo/analyze");
  await page.getByRole("button", { name: "전체 기간", exact: true }).click();
  await expect(page.getByRole("status")).toContainText("GitHub에서 실제 저장소 생성 시각");
  await expect(page.getByLabel("시작일 (Start Date)")).toHaveValue("");
  await page.getByRole("button", { name: "기여 분석 시작하기", exact: true }).click();
  await expect.poll(() => state.requests).toEqual([{ allTime: true }]);
  await expect(page.getByLabel("시작일 (Start Date)")).toHaveValue("2011-04-12");
});

test("switching out of full history restores explicit dates and clamps month-end in UTC", async ({ page }) => {
  await page.clock.setFixedTime(new Date("2026-03-31T12:00:00Z"));
  const state = await setup(page, "2011-04-12T13:45:27Z");
  await page.goto("/repositories/repo/analyze");
  await page.getByRole("button", { name: "전체 기간", exact: true }).click();
  await page.getByRole("button", { name: "최근 1개월", exact: true }).click();
  await expect(page.getByLabel("시작일 (Start Date)")).toHaveValue("2026-02-28");
  await expect(page.getByLabel("시작일 (Start Date)")).toBeEnabled();
  await page.getByRole("button", { name: "기여 분석 시작하기", exact: true }).click();
  await expect.poll(() => state.requests).toEqual([{ periodStart: "2026-02-28T00:00:00.000Z", periodEnd: "2026-04-01T00:00:00.000Z" }]);
  expect(state.branchCalls()).toBe(0);
});
