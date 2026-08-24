import { expect, test } from "@playwright/test";

const repositoryId = "11111111-1111-1111-1111-111111111111";
const jobId = "22222222-2222-2222-2222-222222222222";
const analysisId = "33333333-3333-3333-3333-333333333333";
const certificateId = "44444444-4444-4444-4444-444444444444";
const publicId = "55555555-5555-5555-5555-555555555555";
const contract = "0x1111111111111111111111111111111111111111";
const issuer = "0x2222222222222222222222222222222222222222";
const subject = "0x3333333333333333333333333333333333333333";
const certificateHash = `0x${"a".repeat(64)}`;
const onchainId = `0x${"b".repeat(64)}`;
const issueTransaction = `0x${"c".repeat(64)}`;
const revokeTransaction = `0x${"d".repeat(64)}`;

test("GitHub account to public on-chain verification and revocation", async ({ page }) => {
  let repositorySynced = false;
  let jobPolls = 0;
  let issued = false;
  let issuePolls = 0;
  let revocationSubmitted = false;
  let revocationPolls = 0;
  let revoked = false;

  await page.addInitScript(({ walletAddress, issueHash, revokeHash }) => {
    let transactionCount = 0;
    Object.defineProperty(window, "ethereum", {
      value: {
        request: async ({ method }: { method: string }) => {
          if (method === "eth_requestAccounts" || method === "eth_accounts") return [walletAddress];
          if (method === "eth_chainId") return "0x14a34";
          if (method === "wallet_switchEthereumChain" || method === "wallet_addEthereumChain") return null;
          if (method === "eth_sendTransaction") return transactionCount++ === 0 ? issueHash : revokeHash;
          throw new Error(`Unexpected wallet method: ${method}`);
        },
        on: () => undefined,
        removeListener: () => undefined,
      },
      configurable: true,
    });
  }, { walletAddress: issuer, issueHash: issueTransaction, revokeHash: revokeTransaction });

  await page.route("http://backend.test/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname;
    const method = request.method();
    const json = (body: unknown, status = 200) => route.fulfill({
      status,
      contentType: "application/json",
      body: JSON.stringify(body),
    });

    if (path === "/api/auth/csrf") return json({ headerName: "X-XSRF-TOKEN", parameterName: "_csrf", token: "test" });
    if (path === "/api/auth/me") return json({ userId: "user-1", githubUserId: 1001, githubUsername: "octocat", email: "octocat@example.com" });
    if (path === "/api/repositories" && method === "GET") return json(repositorySynced ? [repository()] : []);
    if (path === "/api/repositories/sync" && method === "POST") { repositorySynced = true; return json([repository()]); }
    if (path === `/api/repositories/${repositoryId}`) return json(repository());
    if (path === `/api/repositories/${repositoryId}/analyses` && method === "POST") return json(job("PENDING", 0));
    if (path === `/api/analysis-jobs/${jobId}`) return json(job(++jobPolls > 1 ? "COMPLETED" : "ANALYZING", jobPolls > 1 ? 100 : 60));
    if (path === `/api/repositories/${repositoryId}/analyses` && method === "GET") return json(jobPolls > 1 ? [analysis()] : []);
    if (path === `/api/analyses/${analysisId}`) return json(analysis());
    if (path === "/api/certificates" && method === "POST") return json(certificate());
    if (path === `/api/certificates/${certificateId}`) return json(certificate());
    if (path === `/api/certificates/${certificateId}/attestation` && method === "GET") {
      if (!issued) return json({ message: "not found" }, 404);
      if (revocationSubmitted && ++revocationPolls > 1) revoked = true;
      if (!revocationSubmitted) issuePolls++;
      return json(attestation(issuePolls > 1 ? "CONFIRMED" : "PENDING",
        revocationSubmitted ? (revoked ? "CONFIRMED" : "PENDING") : null));
    }
    if (path === `/api/certificates/${certificateId}/attestation-intent` && method === "POST") return json(intent("issue", [onchainId, certificateHash, subject]));
    if (path === `/api/certificates/${certificateId}/transactions` && method === "POST") { issued = true; return json(attestation("PENDING", null)); }
    if (path === `/api/certificates/${certificateId}/revocation-intent` && method === "POST") return json(intent("revoke", [onchainId]));
    if (path === `/api/certificates/${certificateId}/revocation-transactions` && method === "POST") {
      revocationSubmitted = true;
      return json(attestation("CONFIRMED", "PENDING"));
    }
    if (path === `/api/public/certificates/${publicId}/verification`) return json({
      publicId,
      status: revoked ? "REVOKED" : "VALID",
      storedHash: certificateHash,
      calculatedHash: certificateHash,
      transactionHash: issueTransaction,
      message: revoked ? "Certificate was revoked on-chain" : "Payload, database, and on-chain hash match",
    });
    if (path === `/api/public/certificates/${publicId}`) return json(certificate());
    return json({ message: `Unhandled ${method} ${path}` }, 500);
  });

  await page.goto("/login");
  await expect(page.getByRole("link", { name: "GitHub로 계속하기" })).toHaveAttribute("href", "http://backend.test/api/auth/github");

  await page.goto("/dashboard");
  await expect(page.getByText("@octocat")).toBeVisible();
  await page.getByRole("link", { name: "저장소" }).click();
  await page.getByRole("button", { name: "GitHub 저장소 동기화" }).click();
  await page.getByRole("link", { name: /octocat\/demo/ }).click();
  await page.getByRole("link", { name: "새 분석" }).click();
  await page.getByRole("button", { name: "분석 시작" }).click();
  await page.getByRole("link", { name: "결과 보기" }).click();
  await expect(page.locator(".score")).toContainText("42");
  await page.getByLabel("Subject 지갑 주소 (온체인 발급 시 필수)").fill(subject);
  await page.getByRole("button", { name: "인증서 만들기" }).click();
  await page.getByRole("link", { name: "인증서 열기" }).click();

  await page.getByRole("button", { name: "온체인 발급" }).click();
  await expect(page.locator("p", { hasText: "발급 트랜잭션:" })).toContainText("CONFIRMED", { timeout: 10_000 });
  await page.getByLabel("폐기 사유").fill("Superseded certificate");
  await page.getByRole("button", { name: "온체인 폐기" }).click();
  await expect(page.locator("p", { hasText: "폐기 트랜잭션:" })).toContainText("CONFIRMED", { timeout: 10_000 });
  await page.getByRole("link", { name: "공개 검증 화면" }).click();
  await expect(page.getByText("REVOKED", { exact: true })).toBeVisible();

  function repository() {
    return { id: repositoryId, githubRepositoryId: 2001, ownerLogin: "octocat", name: "demo",
      fullName: "octocat/demo", url: "https://github.com/octocat/demo", visibility: "PUBLIC",
      defaultBranch: "main", language: "TypeScript", archived: false, lastSyncedAt: "2026-08-18T00:00:00Z" };
  }
  function job(status: string, progress: number) {
    return { id: jobId, repositoryId, periodStart: "2026-07-18T00:00:00Z", periodEnd: "2026-08-18T23:59:59Z",
      collectorVersion: "github-v1", status, progress, errorCode: null, errorMessage: null };
  }
  function analysis() {
    return { id: analysisId, jobId, repositoryId, periodStart: "2026-07-18T00:00:00Z", periodEnd: "2026-08-18T23:59:59Z",
      metrics: { commits: 4, pullRequests: 2, reviews: 1 }, score: 42, scoreVersion: "score-v1",
      calculationRules: "v1", technicalAreas: ["TypeScript"], summary: "기여 요약", aiModel: "rule-based", aiPromptVersion: "v1" };
  }
  function certificate() {
    return { id: certificateId, publicId, analysisId, schemaVersion: "1.0", payload: { analysisId }, hash: certificateHash,
      issuerWalletAddress: issued ? issuer : null, subjectWalletAddress: subject, status: revoked ? "REVOKED" : "ISSUED",
      issuedAt: "2026-08-18T00:00:00Z", revokedAt: revoked ? "2026-08-18T01:00:00Z" : null,
      revocationReason: revoked ? "Superseded certificate" : null };
  }
  function intent(functionName: "issue" | "revoke", args: string[]) {
    return { certificateId, chainId: 84532, network: "base-sepolia", contractAddress: contract,
      functionName, arguments: args, onchainCertificateId: onchainId, certificateHash };
  }
  function attestation(status: string, revocationStatus: string | null) {
    return { id: "attestation-1", certificateId, chainId: 84532, network: "base-sepolia", contractAddress: contract,
      onchainCertificateId: onchainId, transactionHash: issueTransaction, blockNumber: status === "CONFIRMED" ? 42 : null,
      status, submittedAt: "2026-08-18T00:00:00Z", confirmedAt: status === "CONFIRMED" ? "2026-08-18T00:01:00Z" : null,
      revocationTransactionHash: revocationSubmitted ? revokeTransaction : null,
      revocationBlockNumber: revoked ? 43 : null, revocationStatus, revocationReason: revocationSubmitted ? "Superseded certificate" : null,
      revocationSubmittedAt: revocationSubmitted ? "2026-08-18T00:02:00Z" : null,
      revocationConfirmedAt: revoked ? "2026-08-18T00:03:00Z" : null };
  }
});
