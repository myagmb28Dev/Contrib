"use client";

import Link from "next/link";
import { certificateResult } from "@/lib/certificate-result";
import { ComparisonSummary } from "./activity-comparison";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";

import { ApiRequestError, getAppOrigin, getCertificates, type Certificate } from "@/lib/api";

function formatStatus(status: string) {
  switch (status.toUpperCase()) {
    case "ISSUED":
      return { label: "증명서 생성 완료", className: "status-default" };
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

  const [viewMode, setViewMode] = useState<"grid" | "compact">("grid");
  const [selectedRepo, setSelectedRepo] = useState<string>("ALL");


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

  function copyId(publicId: string, id: string) {
    navigator.clipboard.writeText(publicId).then(() => {
      setCopiedId(id);
      setTimeout(() => setCopiedId(null), 2000);
    });
  }

  function copyLink(publicId: string, id: string) {
    const origin = getAppOrigin();
    const url = `${origin}/verify/${publicId}`;
    navigator.clipboard.writeText(url).then(() => {
      setCopiedId(id);
      setTimeout(() => setCopiedId(null), 2000);
    });
  }

  const uniqueRepos = useMemo(() => {
    const set = new Set<string>();
    items.forEach((item) => {
      const name = certificateResult(item).repository;
      if (name) set.add(name);
    });
    return Array.from(set);
  }, [items]);

  const filteredAndSorted = useMemo(() => {
    let result = [...items];

    if (selectedRepo !== "ALL") {
      result = result.filter((item) => {
        const name = certificateResult(item).repository;
        return name === selectedRepo;
      });
    }

    result.sort((a, b) => {
      return new Date(b.issuedAt).getTime() - new Date(a.issuedAt).getTime();
    });

    return result;
  }, [items, selectedRepo]);

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
            기여 분석을 완료한 후 해당 결과에서 공식 기여 인증서를 발급받을 수 있습니다.
          </p>
          <Link className="button primary" href="/dashboard/analyses">
            분석 목록에서 인증서 발급하기
          </Link>
        </div>
      ) : (
        <>
          {/* Controls Toolbar */}
          <div className="list-controls-toolbar">
            <div className="controls-filter-group">
              <select
                className="filter-select"
                value={selectedRepo}
                onChange={(e) => setSelectedRepo(e.target.value)}
                aria-label="저장소 필터"
              >
                <option value="ALL">전체 저장소 ({items.length})</option>
                {uniqueRepos.map((repo) => (
                  <option key={repo} value={repo}>
                    {repo}
                  </option>
                ))}
              </select>
            </div>

            <div className="view-toggle-group">
              <button
                type="button"
                className={`view-toggle-btn ${viewMode === "grid" ? "active" : ""}`}
                onClick={() => setViewMode("grid")}
              >
                카드 뷰
              </button>
              <button
                type="button"
                className={`view-toggle-btn ${viewMode === "compact" ? "active" : ""}`}
                onClick={() => setViewMode("compact")}
              >
                컴팩트 뷰
              </button>
            </div>
          </div>

          {filteredAndSorted.length === 0 ? (
            <div className="card empty-state-card">
              <p className="muted">선택한 필터 조건에 해당하는 인증서가 없습니다.</p>
            </div>
          ) : viewMode === "grid" ? (
            /* Card Grid View */
            <div className="certificates-grid">
              {filteredAndSorted.map((cert) => {
                const statusInfo = formatStatus(cert.status);
                return (
                  <article className="certificate-card" key={cert.id}>
                    <div className="cert-card-top" style={{ alignItems: "flex-start" }}>
                      <div style={{ display: "flex", flexDirection: "column", gap: "2px", minWidth: 0 }}>
                        <span className="cert-label" style={{ fontSize: "0.72rem" }}>Repository</span>
                        <strong style={{ fontSize: "1.05rem", color: "var(--foreground)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                          {certificateResult(cert).repository || "저장소"}
                        </strong>
                      </div>
                      <div style={{ display: "flex", alignItems: "center", gap: "6px", flexShrink: 0 }}>
                        <span className={`verified-badge ${statusInfo.className}`}>
                          {statusInfo.label}
                        </span>
                      </div>
                    </div>

                    <ComparisonSummary comparison={certificateResult(cert).comparison} />
                    <div className="cert-public-id-bar">
                      <span className="cert-id-tag">Public ID</span>
                      <code className="cert-id-val">{cert.publicId}</code>
                      <div className="cert-id-btn-group">
                        <button
                          type="button"
                          className="cert-id-copy-action"
                          onClick={() => copyId(cert.publicId, `id-${cert.id}`)}
                          title="36자리 Public ID 번호 복사"
                        >
                          {copiedId === `id-${cert.id}` ? "ID 복사됨!" : "ID 복사"}
                        </button>
                        <button
                          type="button"
                          className="cert-id-copy-action highlight"
                          onClick={() => copyLink(cert.publicId, `link-${cert.id}`)}
                          title="이력서/포트폴리오용 전체 검증 링크 URL 복사"
                        >
                          {copiedId === `link-${cert.id}` ? "링크 복사됨!" : "링크 복사"}
                        </button>
                      </div>
                    </div>

                    <div className="cert-meta-grid">
                      <div>
                        <span className="meta-label">Subject Wallet</span>
                        <span className="meta-val monospace">
                          {cert.subjectWalletAddress
                            ? `${cert.subjectWalletAddress.slice(0, 8)}...${cert.subjectWalletAddress.slice(-6)}`
                            : "미설정 (오프체인)"}
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
                        인증서 상세 / 온체인 관리 &rarr;
                      </Link>
                    </div>
                  </article>
                );
              })}
            </div>
          ) : (
            /* Compact Table / List View */
            <div className="compact-table-container">
              <table className="compact-table">
                <thead>
                  <tr>
                    <th>저장소</th>
                    <th>실험적 백분위</th>
                    <th>증명서 상태</th>
                    <th>공개 검증 ID (복사)</th>
                    <th>발급 일시</th>
                    <th style={{ textAlign: "right" }}>작업</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredAndSorted.map((cert) => {
                    const statusInfo = formatStatus(cert.status);
                    return (
                      <tr key={cert.id}>
                        <td>
                          <span className="table-repo-badge">
                            {certificateResult(cert).repository || "저장소"}
                          </span>
                        </td>
                        <td>
                          <div className="muted"><ComparisonSummary comparison={certificateResult(cert).comparison} /></div>
                        </td>
                        <td>
                          <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                            <span className={`verified-badge ${statusInfo.className}`} style={{ padding: "2px 6px", fontSize: "0.72rem" }}>
                              {statusInfo.label}
                            </span>
                          </div>
                        </td>
                        <td>
                          <div className="cert-public-id-bar" style={{ maxWidth: "340px", padding: "2px 6px" }}>
                            <code className="cert-id-val" style={{ fontSize: "0.74rem" }}>{cert.publicId}</code>
                            <div className="cert-id-btn-group">
                              <button
                                type="button"
                                className="cert-id-copy-action"
                                onClick={() => copyId(cert.publicId, `id-${cert.id}`)}
                                title="ID 복사"
                              >
                                {copiedId === `id-${cert.id}` ? "복사됨" : "ID"}
                              </button>
                              <button
                                type="button"
                                className="cert-id-copy-action highlight"
                                onClick={() => copyLink(cert.publicId, `link-${cert.id}`)}
                                title="링크 복사"
                              >
                                {copiedId === `link-${cert.id}` ? "복사됨" : "링크"}
                              </button>
                            </div>
                          </div>
                        </td>
                        <td>
                          <span className="table-period">
                            {new Date(cert.issuedAt).toLocaleDateString()}
                          </span>
                        </td>
                        <td style={{ textAlign: "right" }}>
                          <Link
                            className="button primary sm"
                            href={`/certificates/${cert.id}`}
                            style={{ padding: "4px 10px", fontSize: "0.78rem" }}
                          >
                            상세 &rarr;
                          </Link>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </div>
  );
}
