"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";

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

  const [viewMode, setViewMode] = useState<"grid" | "compact">("grid");
  const [selectedRepo, setSelectedRepo] = useState<string>("ALL");
  const [sortBy, setSortBy] = useState<"latest" | "score">("latest");

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

  const uniqueRepos = useMemo(() => {
    const set = new Set<string>();
    items.forEach((item) => {
      const name = item.repositoryName || item.repositoryFullName;
      if (name) set.add(name);
    });
    return Array.from(set);
  }, [items]);

  const filteredAndSorted = useMemo(() => {
    let result = [...items];

    if (selectedRepo !== "ALL") {
      result = result.filter((item) => {
        const name = item.repositoryName || item.repositoryFullName;
        return name === selectedRepo;
      });
    }

    result.sort((a, b) => {
      if (sortBy === "score") {
        return b.score - a.score;
      }
      return new Date(b.periodEnd).getTime() - new Date(a.periodEnd).getTime();
    });

    return result;
  }, [items, selectedRepo, sortBy]);

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

              <select
                className="filter-select"
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as "latest" | "score")}
                aria-label="정렬 기준"
              >
                <option value="latest">최신 분석순</option>
                <option value="score">높은 점수순 (Highest Score)</option>
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
              <p className="muted">선택한 필터 조건에 해당하는 분석 이력이 없습니다.</p>
            </div>
          ) : viewMode === "grid" ? (
            /* Card Grid View */
            <div className="analyses-grid">
              {filteredAndSorted.map((analysis) => {
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
                      <span className="ai-model-tag">AI 분석</span>
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
          ) : (
            /* Compact Table / List View */
            <div className="compact-table-container">
              <table className="compact-table">
                <thead>
                  <tr>
                    <th>저장소</th>
                    <th>기여 점수</th>
                    <th>분석 기간</th>
                    <th>AI 요약 & 기술 영역</th>
                    <th style={{ textAlign: "right" }}>작업</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredAndSorted.map((analysis) => {
                    const tier = getScoreTier(analysis.score);
                    return (
                      <tr key={analysis.id}>
                        <td>
                          <span className="table-repo-badge">
                            {analysis.repositoryName || analysis.repositoryFullName || "저장소"}
                          </span>
                        </td>
                        <td>
                          <span className="table-score-badge">
                            {analysis.score}점
                            <small style={{ opacity: 0.8, fontSize: "0.74rem" }}>({tier.label})</small>
                          </span>
                        </td>
                        <td>
                          <span className="table-period">
                            {new Date(analysis.periodStart).toLocaleDateString()} ~{" "}
                            {new Date(analysis.periodEnd).toLocaleDateString()}
                          </span>
                        </td>
                        <td>
                          <div style={{ display: "flex", flexDirection: "column", gap: "3px" }}>
                            {analysis.summary && (
                              <span className="table-summary-preview" title={analysis.summary}>
                                {analysis.summary}
                              </span>
                            )}
                            {analysis.technicalAreas && analysis.technicalAreas.length > 0 && (
                              <div className="area-tags compact" style={{ margin: 0 }}>
                                {analysis.technicalAreas.slice(0, 4).map((area, idx) => (
                                  <span key={idx} style={{ fontSize: "0.68rem", padding: "1px 5px" }}>{area}</span>
                                ))}
                              </div>
                            )}
                          </div>
                        </td>
                        <td style={{ textAlign: "right" }}>
                          <Link
                            className="button primary sm"
                            href={`/repositories/${analysis.repositoryId}/analysis/${analysis.id}`}
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
