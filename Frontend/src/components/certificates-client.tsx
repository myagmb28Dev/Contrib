"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { ApiRequestError, getCertificates, type Certificate } from "@/lib/api";

function formatStatus(status: string) {
  switch (status.toUpperCase()) {
    case "ISSUED":
    case "CONFIRMED":
    case "VERIFIED":
    case "VALID":
      return { label: "VERIFIED", className: "status-valid" };
    case "PENDING":
      return { label: "PENDING", className: "status-pending" };
    case "REVOKED":
      return { label: "REVOKED", className: "status-revoked" };
    default:
      return { label: status, className: "status-default" };
  }
}

export function CertificatesClient() {
  const router = useRouter();
  const [items, setItems] = useState<Certificate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [copiedId, setCopiedId] = useState<string | null>(null);

  useEffect(() => {
    getCertificates()
      .then(setItems)
      .catch((reason: unknown) => {
        if (reason instanceof ApiRequestError && reason.status === 401) {
          router.replace("/");
          return;
        }
        setError(reason instanceof Error ? reason.message : "인증서 목록을 불러오지 못했습니다.");
      })
      .finally(() => setLoading(false));
  }, [router]);

  function copyHash(hash: string, id: string) {
    navigator.clipboard.writeText(hash).then(() => {
      setCopiedId(id);
      setTimeout(() => setCopiedId(null), 2000);
    });
  }

  return (
    <div className="stack full-width">
      {error && <p className="error-message">{error}</p>}

      {loading ? (
        <div className="card loading-card">
          <div className="skeleton-line lg" />
          <div className="skeleton-line md" />
          <p className="muted">인증서 목록을 불러오는 중입니다...</p>
        </div>
      ) : items.length === 0 ? (
        <div className="card empty-state-card">
          <h3>발급된 인증서가 없습니다</h3>
          <p className="muted">
            기여 분석을 완료한 후 해당 결과에서 온체인 Contribution Certificate를 발급받을 수 있습니다.
          </p>
          <Link className="button primary" href="/dashboard/analyses">
            분석 목록에서 인증서 발급하기
          </Link>
        </div>
      ) : (
        <div className="certificates-grid">
          {items.map((cert) => {
            const statusInfo = formatStatus(cert.status);
            return (
              <article className="certificate-card" key={cert.id}>
                <div className="cert-card-top">
                  <span className={`verified-badge ${statusInfo.className}`}>
                    {statusInfo.label}
                  </span>
                  <span className="network-tag">Base Sepolia</span>
                </div>

                <div className="cert-hash-section">
                  <span className="cert-label">Certificate Hash</span>
                  <div className="hash-copy-row">
                    <code className="monospace">{cert.hash}</code>
                    <button
                      type="button"
                      className="copy-btn"
                      onClick={() => copyHash(cert.hash, cert.id)}
                      title="해시 복사"
                    >
                      {copiedId === cert.id ? "복사됨" : "복사"}
                    </button>
                  </div>
                </div>

                <div className="cert-meta-grid">
                  <div>
                    <span className="meta-label">Subject Wallet</span>
                    <span className="meta-val monospace">
                      {cert.subjectWalletAddress
                        ? `${cert.subjectWalletAddress.slice(0, 8)}...${cert.subjectWalletAddress.slice(-6)}`
                        : "미설정"}
                    </span>
                  </div>
                  <div>
                    <span className="meta-label">발급 일시</span>
                    <span className="meta-val">
                      {new Date(cert.issuedAt).toLocaleDateString()}
                    </span>
                  </div>
                </div>

                <div className="cert-card-actions">
                  <Link
                    className="button primary sm"
                    href={`/certificates/${cert.id}`}
                  >
                    인증서 상세 / 온체인 발행 &rarr;
                  </Link>
                  <Link
                    className="button sm"
                    href={`/verify/${cert.publicId}`}
                  >
                    공개 검증
                  </Link>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}
