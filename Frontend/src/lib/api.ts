export const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export type CurrentUser = {
  userId: string;
  githubUserId: number;
  githubUsername: string;
  email: string | null;
};

type CsrfToken = {
  headerName: string;
  parameterName: string;
  token: string;
};

export type Repository = {
  id: string;
  githubRepositoryId: number;
  ownerLogin: string;
  name: string;
  fullName: string;
  url: string;
  visibility: string;
  defaultBranch: string;
  language: string | null;
  archived: boolean;
  lastSyncedAt: string;
};

export type AnalysisJob = {
  id: string;
  repositoryId: string;
  periodStart: string;
  periodEnd: string;
  collectorVersion: string;
  status: string;
  progress: number;
  errorCode: string | null;
  errorMessage: string | null;
};

export type Analysis = {
  id: string;
  jobId: string;
  repositoryId: string;
  periodStart: string;
  periodEnd: string;
  metrics: Record<string, number>;
  score: number;
  scoreVersion: string;
  calculationRules: string;
  technicalAreas: string[];
  summary: string | null;
  aiModel: string | null;
  aiPromptVersion: string | null;
};

export type Certificate = {
  id: string;
  publicId: string;
  analysisId: string;
  schemaVersion: string;
  payload: Record<string, unknown>;
  hash: `0x${string}`;
  issuerWalletAddress: `0x${string}` | null;
  subjectWalletAddress: `0x${string}` | null;
  status: string;
  issuedAt: string;
  revokedAt: string | null;
  revocationReason: string | null;
};

export type AttestationIntent = {
  certificateId: string;
  chainId: number;
  network: string;
  contractAddress: `0x${string}`;
  functionName: "issue";
  arguments: [`0x${string}`, `0x${string}`, `0x${string}`];
  onchainCertificateId: `0x${string}`;
  certificateHash: `0x${string}`;
};

export type Attestation = {
  transactionHash: `0x${string}`;
  blockNumber: number | null;
  status: string;
};

export type Verification = {
  publicId: string;
  status: string;
  storedHash: string | null;
  calculatedHash: string | null;
  transactionHash: string | null;
  message: string;
};

export async function getCurrentUser(signal?: AbortSignal): Promise<CurrentUser> {
  const response = await fetch(`${apiBaseUrl}/api/auth/me`, {
    credentials: "include",
    signal,
  });

  if (!response.ok) {
    throw new ApiRequestError(response.status, "현재 사용자 정보를 불러오지 못했습니다.");
  }

  return response.json() as Promise<CurrentUser>;
}

export async function logout(): Promise<void> {
  const response = await apiFetch("/api/auth/logout", { method: "POST" });

  if (!response.ok) {
    throw new ApiRequestError(response.status, "로그아웃하지 못했습니다.");
  }
}

export async function getRepositories(): Promise<Repository[]> {
  return apiJson("/api/repositories");
}

export async function syncRepositories(): Promise<Repository[]> {
  return apiJson("/api/repositories/sync", { method: "POST" });
}

export async function getRepository(id: string): Promise<Repository> {
  return apiJson(`/api/repositories/${id}`);
}

export async function getRepositoryAnalyses(repositoryId: string): Promise<Analysis[]> {
  return apiJson(`/api/repositories/${repositoryId}/analyses`);
}

export async function getAnalyses(): Promise<Analysis[]> {
  return apiJson("/api/analyses");
}

export async function createAnalysis(
  repositoryId: string,
  periodStart: string,
  periodEnd: string,
): Promise<AnalysisJob> {
  return apiJson(`/api/repositories/${repositoryId}/analyses`, {
    method: "POST",
    body: JSON.stringify({ periodStart, periodEnd }),
  });
}

export async function getAnalysisJob(jobId: string): Promise<AnalysisJob> {
  return apiJson(`/api/analysis-jobs/${jobId}`);
}

export async function getAnalysis(analysisId: string): Promise<Analysis> {
  return apiJson(`/api/analyses/${analysisId}`);
}

export async function createCertificate(
  analysisId: string,
  subjectWalletAddress: string | null,
): Promise<Certificate> {
  return apiJson("/api/certificates", {
    method: "POST",
    body: JSON.stringify({ analysisId, subjectWalletAddress }),
  });
}

export async function getCertificates(): Promise<Certificate[]> {
  return apiJson("/api/certificates");
}

export async function getCertificate(id: string): Promise<Certificate> {
  return apiJson(`/api/certificates/${id}`);
}

export async function getAttestationIntent(id: string): Promise<AttestationIntent> {
  return apiJson(`/api/certificates/${id}/attestation-intent`, { method: "POST" });
}

export async function submitAttestation(
  id: string,
  transactionHash: string,
  issuerWalletAddress: string,
): Promise<Attestation> {
  return apiJson(`/api/certificates/${id}/transactions`, {
    method: "POST",
    body: JSON.stringify({ transactionHash, issuerWalletAddress }),
  });
}

export async function getPublicCertificate(publicId: string): Promise<Certificate> {
  return apiJson(`/api/public/certificates/${publicId}`);
}

export async function verifyCertificate(publicId: string): Promise<Verification> {
  return apiJson(`/api/public/certificates/${publicId}/verification`);
}

async function apiJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await apiFetch(path, init);
  if (!response.ok) {
    const error = (await response.json().catch(() => null)) as { message?: string } | null;
    throw new ApiRequestError(response.status, error?.message ?? "API 요청에 실패했습니다.");
  }
  return response.json() as Promise<T>;
}

async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers);
  if (init.body) {
    headers.set("Content-Type", "application/json");
  }
  if (init.method && !["GET", "HEAD", "OPTIONS"].includes(init.method.toUpperCase())) {
    const csrf = await getCsrfToken();
    headers.set(csrf.headerName, csrf.token);
  }
  return fetch(`${apiBaseUrl}${path}`, { ...init, credentials: "include", headers });
}

async function getCsrfToken(): Promise<CsrfToken> {
  const response = await fetch(`${apiBaseUrl}/api/auth/csrf`, { credentials: "include" });
  if (!response.ok) {
    throw new ApiRequestError(response.status, "CSRF 토큰을 발급받지 못했습니다.");
  }
  return response.json() as Promise<CsrfToken>;
}

export class ApiRequestError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = "ApiRequestError";
  }
}
