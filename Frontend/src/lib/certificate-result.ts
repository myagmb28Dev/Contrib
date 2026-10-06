import type { ActivityComparison, Certificate } from "./api";

function record(value: unknown): Record<string, unknown> | null {
  return value !== null && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : null;
}

export function certificateResult(certificate: Certificate) {
  const payload = certificate.payload;
  const result = record(payload.result) ?? payload; // Historical flat fixtures and old exports remain readable.
  const repo = record(payload.repository);
  const period = record(payload.period);
  return {
    repository: certificate.repositoryName || certificate.repositoryFullName ||
      (typeof repo?.fullName === "string" ? repo.fullName : typeof payload.repository === "string" ? payload.repository : "GitHub Repository"),
    comparison: record(result.activityComparison) as ActivityComparison | null,
    analysisPeriod: typeof period?.start === "string" && typeof period?.end === "string"
      ? { start: period.start, end: period.end } : null,
  };
}
