"use client";

import { useEffect, useState } from "react";
import { getPublicCertificate, verifyCertificate, type Certificate, type Verification } from "@/lib/api";

export function VerificationClient({ publicId }: { publicId: string }) {
  const [certificate, setCertificate] = useState<Certificate | null>(null);
  const [verification, setVerification] = useState<Verification | null>(null);
  const [error, setError] = useState("");
  useEffect(() => {
    verifyCertificate(publicId).then(async (result) => {
      setVerification(result);
      if (result.status !== "NOT_FOUND") setCertificate(await getPublicCertificate(publicId));
    }).catch((reason: Error) => setError(reason.message));
  }, [publicId]);
  if (error) return <p className="error-message">{error}</p>;
  if (!verification || (!certificate && verification.status !== "NOT_FOUND")) return <p className="muted">검증 중입니다...</p>;
  return <div className="stack full-width">
    <div className={`verification-status status-${verification.status.toLowerCase()}`}>{verification.status}</div>
    <p>{verification.message}</p>
    <dl className="identity-list"><div><dt>저장된 Hash</dt><dd className="monospace">{verification.storedHash}</dd></div>
      <div><dt>계산된 Hash</dt><dd className="monospace">{verification.calculatedHash}</dd></div>
      <div><dt>트랜잭션</dt><dd className="monospace">{verification.transactionHash ?? "없음"}</dd></div></dl>
    {certificate && <details className="full-width"><summary>공개 payload</summary><pre>{JSON.stringify(certificate.payload, null, 2)}</pre></details>}
  </div>;
}
