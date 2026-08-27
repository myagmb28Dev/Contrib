"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { Breadcrumb } from "./breadcrumb";
import { getRepository, getRepositoryAnalyses, type Analysis, type Repository } from "@/lib/api";

function getScoreTier(score: number) {
  if (score >= 80) return { label: "Excellent", className: "tier-high" };
  if (score >= 60) return { label: "Good", className: "tier-mid" };
  return { label: "Developing", className: "tier-low" };
}

export function RepositoryOverviewClient({ repositoryId }: { repositoryId: string }) {
  const [repository, setRepository] = useState<Repository | null>(null);
  const [analyses, setAnalyses] = useState<Analysis[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([getRepository(repositoryId), getRepositoryAnalyses(repositoryId)])
      .then(([repo, values]) => {
        setRepository(repo);
        setAnalyses(values);
      })
      .catch((reason: Error) => setError(reason.message))
      .finally(() => setLoading(false));
  }, [repositoryId]);

  if (error) {
    return (
      <div className="card stack">
        <p className="error-message">{error}</p>
        <Link href="/dashboard/repositories" className="button">
          저장소 목록으로 돌아가기
        </Link>
      </div>
    );
  }

  if (loading || !repository) {
    return (
      <div className="card loading-card">
        <div className="skeleton-line lg" />
        <div className="skeleton-line md" />
        <p className="muted">저장소 정보를 불러오는 중입니다...</p>
      </div>
    );
  }

  return (
    <div className="stack full-width">
      <Breadcrumb
        items={[
          { label: "대시보드", href: "/dashboard" },
          { label: "저장소 관리", href: "/dashboard/repositories" },
          { label: repository.fullName },
        ]}
      />

      {/* Repo Hero Card */}
      <section className="card repo-hero-card">
        <div className="repo-hero-main">
          <div className="repo-title-box">
            <span className="repo-hero-icon">📁</span>
            <div>
              <h2>{repository.name}</h2>
              <span className="muted">소유자: @{repository.ownerLogin} · {repository.visibility}</span>
            </div>
          </div>
          <div className="repo-hero-actions">
            <Link
              className="button primary"
              href={`/repositories/${repositoryId}/analyze`}
            >
              ⚡ 새 기여 분석 시작
            </Link>
            <a
              className="button"
              href={repository.url}
              target="_blank"
              rel="noreferrer"
            >
              GitHub에서 보기 &rarr;
            </a>
          </div>
        </div>

        <div className="repo-meta-pills">
          <div className="meta-pill">
            <span className="pill-label">주요 언어</span>
            <strong className="pill-val">{repository.language ?? "언어 미지정"}</strong>
          </div>
          <div className="meta-pill">
            <span className="pill-label">기본 브랜치</span>
            <strong className="pill-val">🌿 {repository.defaultBranch}</strong>
          </div>
          <div className="meta-pill">
            <span className="pill-label">최근 동기화</span>
            <strong className="pill-val">
              {new Date(repository.lastSyncedAt).toLocaleDateString()}
            </strong>
          </div>
        </div>
      </section>

      {/* Completed Analyses Section */}
      <section className="analyses-history-section full-width">
        <div className="section-header-row">
          <h3>이 저장소의 기여 분석 이력</h3>
          <span className="muted">{analyses.length}건 완료됨</span>
        </div>

        {analyses.length === 0 ? (
          <div className="card empty-state-card">
            <div className="empty-badge-icon">📊</div>
            <h4>아직 진행된 기여 분석이 없습니다</h4>
            <p className="muted">
              기간을 선택하여 커밋, PR, 코드 리뷰 및 변경 라인 수 기반의 기여도를 분석해 보세요.
            </p>
            <Link
              className="button primary"
              href={`/repositories/${repositoryId}/analyze`}
            >
              첫 번째 기여 분석 시작
            </Link>
          </div>
        ) : (
          <div className="analyses-grid">
            {analyses.map((analysis) => {
              const tier = getScoreTier(analysis.score);
              return (
                <article className="analysis-card" key={analysis.id}>
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
                    <span className="version-tag">{analysis.scoreVersion}</span>
                  </div>

                  <div className="analysis-period-box">
                    <span className="period-icon">📅</span>
                    <span className="period-text">
                      {new Date(analysis.periodStart).toLocaleDateString()} ~{" "}
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
                      🤖 {analysis.aiModel || "Google Gemini 2.0 Flash"}
                    </span>
                    <Link
                      className="button primary sm"
                      href={`/repositories/${repositoryId}/analysis/${analysis.id}`}
                    >
                      상세 보기 &rarr;
                    </Link>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
