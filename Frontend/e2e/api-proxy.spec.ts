import { expect, test } from "@playwright/test";

test("same-origin API proxy preserves cookies, CSRF, body and query", async ({ request }) => {
  const response = await request.post("/api/proxy-test?period=90", {
    headers: { Cookie: "JSESSIONID=existing-session", "X-XSRF-TOKEN": "csrf-value" },
    data: { repository: "demo" },
  });
  expect(response.status()).toBe(200);
  expect(response.headers()["x-request-id"]).toBe("proxy-request");
  expect(await response.json()).toEqual({
    url: "/api/proxy-test?period=90", method: "POST", cookie: "JSESSIONID=existing-session",
    csrf: "csrf-value", body: JSON.stringify({ repository: "demo" }),
  });
});

test("OAuth redirect and session cookie survive the same-origin proxy", async ({ request }) => {
  const response = await request.get("/oauth2/authorization/github", { maxRedirects: 0 });
  expect(response.status()).toBe(302);
  expect(response.headers().location).toBe("https://github.com/login/oauth/authorize?state=proxy-test");
  expect(response.headers()["set-cookie"]).toContain("JSESSIONID=proxy-session");
  expect(response.headers()["set-cookie"]).toContain("HttpOnly");
});
