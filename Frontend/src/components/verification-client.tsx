"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { getPublicCertificate, verifyCertificate, type Certificate, type Verification } from "@/lib/api";
import { verificationStatusLabel } from "@/lib/verification-status";

export function VerificationClient({ publicId }: { publicId: string }) {
  const [certificate, setCertificate] = useState<Certificate | null>(null);
  const [verification, setVerification] = useState<Verification | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    verifyCertificate(publicId)
      .then(async (result) => {
        setVerification(result);
        if (result.status !== "NOT_FOUND") {
          try {
            setCertificate(await getPublicCertificate(publicId));
          } catch {
            // Ignore certificate fetch error if verify succeeded
          }
        }
      })
      .catch((reason: Error) => setError(reason.message))
      .finally(() => setLoading(false));
  }, [publicId]);

  if (error) {
    return (
      <div className="card stack">
        <p className="error-message">{error}</p>
        <Link className="button" href="/">
          홈으로 돌아가기
        </Link>
      </div>
    );
  }

  if (loading || !verification) {
    return (
      <div className="card loading-card">
        <div className="skeleton-line lg" />
        <div className="skeleton-line md" />
        <p className="muted">온체인 및 해시 무결성을 검증하는 중입니다...</p>
      </div>
    );
  }

  const isValid = verification.status === "VALID";
  const isPending = verification.status === "PENDING";
  const isNotRegistered = verification.status === "NOT_REGISTERED";
  const isRevoked = verification.status === "REVOKED";

  return (
    <div className="stack full-width">
      {/* Verification Status Banner */}
      <section className="card verification-hero-card">
        <div className="verify-status-row">
          <div
            className={`verification-status-pill ${
              isValid
                ? "status-valid"
                : isNotRegistered
                ? "status-default"
                : isPending
                ? "status-pending"
                : "status-revoked"
            }`}
          >
            {verificationStatusLabel(verification.status)}
          </div>
          {!isNotRegistered && verification.transactionHash && (
            <span className="network-pill">Base Sepolia (Chain ID: 84532)</span>
          )}
        </div>

        <h2 className="verify-title">
          {isValid
            ? "온체인 검증이 완료된 기여 증명서입니다"
            : isNotRegistered
            ? "증명서 생성이 완료되었습니다"
            : isPending
            ? "온체인 등록을 처리하고 있습니다"
            : isRevoked
            ? "폐기(Revoked)된 인증서입니다"
            : "검증 결과 안내"}
        </h2>
        <p className="verify-message-text muted">{isNotRegistered
          ? "저장된 증명서 내용과 해시가 일치합니다. 온체인 등록은 선택 사항이며, 등록하지 않아도 증명서를 조회하고 공유할 수 있습니다."
          : verification.message}</p>
      </section>

      {/* Hash Verification Breakdown */}
      <section className="card full-width">
        <div className="card-header-simple">
          <h3>암호학적 해시 무결성 검증</h3>
          <p className="muted">
            증명서 내용으로 계산한 해시를 저장된 해시와 비교합니다. 온체인 등록 시 블록체인 기록도 함께 확인합니다.
          </p>
        </div>

        <dl className="identity-list">
          <div>
            <dt>공개 검증 ID</dt>
            <dd className="monospace">{verification.publicId}</dd>
          </div>
          <div>
            <dt>저장된 해시 (Stored)</dt>
            <dd className="monospace">{verification.storedHash ?? "없음"}</dd>
          </div>
          <div>
            <dt>계산된 해시 (Calculated)</dt>
            <dd className="monospace">{verification.calculatedHash ?? "없음"}</dd>
          </div>
          <div>
            <dt>온체인 트랜잭션</dt>
            <dd className="monospace">
              {verification.transactionHash ? (
                <a
                  href={`https://sepolia.basescan.org/tx/${verification.transactionHash}`}
                  target="_blank"
                  rel="noreferrer"
                  className="tx-link"
                >
                  {verification.transactionHash} (Basescan)
                </a>
              ) : (
                "등록된 트랜잭션 없음"
              )}
            </dd>
          </div>
        </dl>
      </section>

      {/* Public Payload Viewer */}
      {certificate && (
        <details className="full-width card">
          <summary>인증서 공개 페이로드 (Public Payload)</summary>
          <pre>{JSON.stringify(certificate.payload, null, 2)}</pre>
        </details>
      )}

      <div className="actions">
        <Link className="button primary" href="/">
          Contrib 홈으로 이동
        </Link>
      </div>
    </div>
  );
}
