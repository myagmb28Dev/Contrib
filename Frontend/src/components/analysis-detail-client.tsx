"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { Breadcrumb } from "./breadcrumb";
import { ActivityComparisonCard } from "./activity-comparison";
import { ApiRequestError, createCertificate, getAnalysis, type Analysis } from "@/lib/api";

const metricLabels: Record<string, { label: string; unit?: string }> = {
  commits: { label: "총 커밋 수", unit: "회" },
  pullRequestsOpened: { label: "생성한 PR", unit: "개" },
  reviews: { label: "코드 리뷰", unit: "건" },
  changedFiles: { label: "변경 파일 수 합계", unit: "개" },
  additions: { label: "추가된 라인", unit: "줄" },
  deletions: { label: "삭제된 라인", unit: "줄" },
  pullRequestsCreated: { label: "생성한 PR", unit: "개" },
  pullRequestsMerged: { label: "병합된 PR", unit: "개" },
  pullRequestReviews: { label: "코드 리뷰", unit: "건" },
  linesAdded: { label: "추가된 라인", unit: "줄" },
  linesDeleted: { label: "삭제된 라인", unit: "줄" },
  activeDays: { label: "기여 활동 일수", unit: "일" },
  comments: { label: "이슈/PR 댓글", unit: "개" },
};


export function AnalysisDetailClient({ analysisId }: { analysisId: string }) {
  const router = useRouter();
  const [analysis, setAnalysis] = useState<Analysis | null>(null);
  const [certificateId, setCertificateId] = useState<string | null>(null);
  const [issuing, setIssuing] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    getAnalysis(analysisId)
      .then(setAnalysis)
      .catch((reason: unknown) => {
        if (reason instanceof ApiRequestError && reason.status === 401) {
          router.replace("/");
          return;
        }
        setError(reason instanceof Error ? reason.message : "분석 결과를 불러오지 못했습니다.");
      });
  }, [analysisId, router]);

  async function issue() {
    setError("");
    setIssuing(true);
    try {
      const cert = await createCertificate(analysisId, null);
      setCertificateId(cert.id);
    } catch (reason) {
      if (reason instanceof ApiRequestError && reason.status === 401) {
        router.replace("/");
        return;
      }
      setError(reason instanceof Error ? reason.message : "인증서를 발급하지 못했습니다.");
    } finally {
      setIssuing(false);
    }
  }

  if (error && !analysis) {
    return (
      <div className="card stack">
        <p className="error-message">{error}</p>
        <Link href="/dashboard/analyses" className="button">
          분석 목록으로 돌아가기
        </Link>
      </div>
    );
  }

  if (!analysis) {
    return (
      <div className="card loading-card">
        <div className="skeleton-line lg" />
        <div className="skeleton-line md" />
        <p className="muted">기여 분석 결과를 불러오는 중입니다...</p>
      </div>
    );
  }


  return (
    <div className="stack full-width">
      <Breadcrumb
        items={[
          { label: "대시보드", href: "/dashboard" },
          { label: "기여 분석 목록", href: "/dashboard/analyses" },
          { label: `분석 결과 (${analysis.score}점)` },
        ]}
      />

      {/* Hero Score & AI Summary Card */}
      <ActivityComparisonCard comparison={analysis.activityComparison} />
      <section className="card analysis-hero-card">
        <div className="analysis-score-block">
          <span className="score-hero-label">실험적 활동 점수 v1</span>
          <div className="score-hero-val-row">
            <strong className="score-hero-num">{analysis.score}</strong>
            <div className="score-hero-meta">
              <span>/ 100</span>
              <span className="tier-badge">활동량 요약 · 실력 평가 아님</span>
            </div>
          </div>
          <span className="version-info muted">규칙 버전: {analysis.scoreVersion}</span>
          <details><summary>기존 점수 산정 기준</summary><p>커밋 최대 25점, PR 생성·병합 최대 30점,
            리뷰 최대 20점, 활동 일수 최대 15점, 변경 파일 수 최대 10점입니다. 100점은 각 항목의 상한을 채웠다는 뜻입니다.</p>
            <p>{analysis.calculationRules}</p></details>
        </div>

        <div className="analysis-ai-block">
          <div className="ai-block-header">
            <span className="ai-badge">AI 분석</span>
            <span className="analysis-period-tag">
              {new Date(analysis.periodStart).toLocaleDateString()} ~{" "}
              {new Date(analysis.periodEnd).toLocaleDateString()}
            </span>
          </div>

          <p className="ai-summary-content">
            {analysis.summary || "기여 활동 요약을 생성하지 못했습니다."}
          </p>

          {analysis.technicalAreas && analysis.technicalAreas.length > 0 && (
            <div className="area-tags">
              {analysis.technicalAreas.map((area, idx) => (
                <span key={idx}>{area}</span>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* Metrics Breakdown Grid */}
      <section className="card full-width">
        <div className="card-header-simple">
          <h3>상세 기여 지표</h3>
          <p className="muted">분석 기간 동안 GitHub API를 통해 수집된 실제 활동 지표입니다.</p>
        </div>

        <div className="metrics-expanded-grid">
          {Object.entries(analysis.metrics).map(([key, value]) => {
            const meta = metricLabels[key] || {
              label: key,
              unit: "",
            };
            return (
              <div className="metric-box" key={key}>
                <div className="metric-box-top">
                  <span className="metric-box-label">{meta.label}</span>
                </div>
                <strong className="metric-box-val">
                  {value.toLocaleString()}
                  {meta.unit && <small> {meta.unit}</small>}
                </strong>
              </div>
            );
          })}
        </div>
      </section>

      {/* Certificate Issuance Section */}
      <section className="card certificate-issue-card full-width">
        <div className="issue-card-header">
          <h3>기여 활동 인증서 발급</h3>
          <p className="muted">
            분석 결과와 비교 기준을 고정한 인증서를 발급합니다. 해시 검증은 기록의 무결성을 확인하며, 점수의 객관성이나 개발 실력을 공인하지 않습니다.
          </p>
        </div>

        <div className="issue-action-row mt" style={{ display: "flex", gap: "12px", alignItems: "center" }}>
          {!certificateId ? (
            <button
              className="button primary"
              onClick={issue}
              disabled={issuing}
            >
              {issuing ? "인증서 발급 중..." : "기여 인증서 발급하기"}
            </button>
          ) : (
            <div style={{ display: "flex", gap: "12px", alignItems: "center", flexWrap: "wrap" }}>
              <span className="badge-success" style={{ fontWeight: 600, color: "#059669", background: "#ecfdf5", padding: "6px 14px", borderRadius: "8px", border: "1px solid #a7f3d0" }}>
                인증서 발급 완료
              </span>
              <Link className="button primary" href={`/certificates/${certificateId}`}>
                발급된 인증서 상세 / 온체인 관리 &rarr;
              </Link>
            </div>
          )}
        </div>

        {error && <p className="error-message mt">{error}</p>}
      </section>
    </div>
  );
}
