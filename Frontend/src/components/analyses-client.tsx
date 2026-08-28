"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { ApiRequestError, getAnalyses, type Analysis } from "@/lib/api";

function getScoreTier(score: number) {
  if (score >= 80) return { label: "Excellent", className: "tier-high" };
  if (score >= 60) return { label: "Good", className: "tier-mid" };
  return { label: "Developing", className: "tier-low" };
}

export function AnalysesClient() {
  const router = useRouter();
  const [items, setItems] = useState<Analysis[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    getAnalyses()
      .then(setItems)
      .catch((reason: unknown) => {
        if (reason instanceof ApiRequestError && reason.status === 401) {
          router.replace("/");
          return;
        }
        setError(reason instanceof Error ? reason.message : "분석 이력을 불러오지 못했습니다.");
      })
      .finally(() => setLoading(false));
  }, [router]);

  return (
    <div className="stack full-width">
      {error && <p className="error-message">{error}</p>}

      {loading ? (
        <div className="card loading-card">
          <div className="skeleton-line lg" />
          <div className="skeleton-line md" />
          <p className="muted">기여 분석 이력을 불러오는 중입니다...</p>
        </div>
      ) : items.length === 0 ? (
        <div className="card empty-state-card">
          <h3>완료된 기여 분석이 없습니다</h3>
          <p className="muted">
            동기화된 저장소를 선택하여 첫 번째 GitHub 기여 분석을 시작해 보세요.
          </p>
          <Link className="button primary" href="/dashboard/repositories">
            저장소에서 분석 시작하기
          </Link>
        </div>
      ) : (
        <div className="analyses-grid">
          {items.map((analysis) => {
            const tier = getScoreTier(analysis.score);
            return (
              <article className="analysis-card" key={analysis.id}>
                <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "10px" }}>
                  <strong style={{ fontSize: "1.05rem", color: "var(--foreground)" }}>
                    {analysis.repositoryName || analysis.repositoryFullName || "저장소"}
                  </strong>
                  <span className="version-tag">{analysis.scoreVersion}</span>
                </div>

                <div className="analysis-card-header">
                  <div className="score-badge-box">
                    <span className={`score-badge ${tier.className}`}>
                      {analysis.score}
                    </span>
                    <div className="score-sub">
                      <strong>/ 100</strong>
                      <span className="tier-label">{tier.label}</span>
                    </div>
                  </div>
                </div>

                <div className="analysis-period-box">
                  <span className="period-text">
                    기간: {new Date(analysis.periodStart).toLocaleDateString()} ~{" "}
                    {new Date(analysis.periodEnd).toLocaleDateString()}
                  </span>
                </div>

                {analysis.summary && (
                  <p className="analysis-summary-text">{analysis.summary}</p>
                )}

                {analysis.technicalAreas && analysis.technicalAreas.length > 0 && (
                  <div className="area-tags compact">
                    {analysis.technicalAreas.map((area, idx) => (
                      <span key={idx}>{area}</span>
                    ))}
                  </div>
                )}

                <div className="analysis-card-footer">
                  <span className="ai-model-tag">
                    AI: {analysis.aiModel || "Google Gemini 2.0 Flash"}
                  </span>
                  <Link
                    className="button primary sm"
                    href={`/repositories/${analysis.repositoryId}/analysis/${analysis.id}`}
                  >
                    결과 상세 &rarr;
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
