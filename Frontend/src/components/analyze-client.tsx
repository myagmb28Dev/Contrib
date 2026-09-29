"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";

import { Breadcrumb } from "./breadcrumb";
import {
  ApiRequestError,
  createAnalysis,
  getAnalysisJob,
  getRepository,
  getRepositoryAnalyses,
  type AnalysisJob,
  type Repository,
} from "@/lib/api";

export function AnalyzeClient({ repositoryId }: { repositoryId: string }) {
  const router = useRouter();

  const [repository, setRepository] = useState<Repository | null>(null);

  const [job, setJob] = useState<AnalysisJob | null>(null);
  const [analysisId, setAnalysisId] = useState<string | null>(null);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getRepository(repositoryId)
      .then(setRepository)
      .catch((err: unknown) => {
        if (err instanceof ApiRequestError && err.status === 401) {
          router.replace("/");
          return;
        }
        setError(err instanceof Error ? err.message : "저장소 정보를 불러오지 못했습니다.");
      });
  }, [repositoryId, router]);

  useEffect(() => {
    if (!job || ["COMPLETED", "FAILED", "CANCELLED"].includes(job.status)) return;
    const timer = window.setInterval(async () => {
      try {
        const next = await getAnalysisJob(job.id);
        setJob(next);
        if (next.status === "COMPLETED") {
          const analyses = await getRepositoryAnalyses(repositoryId);
          setAnalysisId(analyses.find((analysis) => analysis.jobId === next.id)?.id ?? null);
        }
      } catch {
        // Retry next interval
      }
    }, 1200);
    return () => window.clearInterval(timer);
  }, [job, repositoryId]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError("");
    setAnalysisId(null);
    setSubmitting(true);
    try {
      const created = await createAnalysis(repositoryId);
      setJob(created);
      setRepository(previous => previous ? { ...previous, githubCreatedAt: created.periodStart } : previous);
      if (created.status === "COMPLETED") {
        const analyses = await getRepositoryAnalyses(repositoryId);
        setAnalysisId(analyses.find((analysis) => analysis.jobId === created.id)?.id ?? null);
      }
    } catch (reason) {
      if (reason instanceof ApiRequestError && reason.status === 401) {
        router.replace("/");
        return;
      }
      setError(reason instanceof Error ? reason.message : "분석을 시작하지 못했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  const isWorking = submitting || (!!job && !["COMPLETED", "FAILED", "CANCELLED"].includes(job.status));

  return (
    <div className="stack full-width">
      <Breadcrumb
        items={[
          { label: "대시보드", href: "/dashboard" },
          { label: "저장소 관리", href: "/dashboard/repositories" },
          {
            label: repository ? repository.name : "저장소 개요",
            href: `/repositories/${repositoryId}/overview`,
          },
          { label: "기여 분석" },
        ]}
      />

      <div className="card analyze-form-card">
        <div className="analyze-card-header">
          <h2>GitHub 기여 분석 실행</h2>
          <p className="muted">
            main 브랜치에서 저장소 생성일부터 현재까지 전체 기간을 자동으로 분석합니다.
          </p>
        </div>

        <div className="analysis-info-box">
          <div>
            <strong>분석 기간 · 전체 기간</strong>
            <p role="status">{repository?.githubCreatedAt
              ? `${repository.githubCreatedAt}부터 분석 요청 시점까지`
              : "분석 시작 시 GitHub에서 실제 저장소 생성 시각을 자동으로 확인합니다."}</p>
            <p className="muted">백분위는 전체 분석 기간과 일치하는 비교 데이터가 있을 때 제공됩니다.
              비교 데이터가 부족하면 원시 활동 지표만 표시합니다.</p>
          </div>
        </div>

        <form className="form-grid" onSubmit={submit}>
          <div className="analysis-info-box">
            <div>
              <strong>수집 및 분석 지표</strong>
              <p>
                커밋(수정/추가 라인 수), 풀 리퀘스트 생성 및 병합, 코드 리뷰 제출, 활동 일수를 포괄하여 AI 요약을 생성합니다.
              </p>
            </div>
          </div>

          <button
            className="button primary hero-action-btn"
            type="submit"
            disabled={isWorking}
          >
            {isWorking ? "분석 작업 진행 중..." : "기여 분석 시작하기"}
          </button>
        </form>

        {job && (
          <div className="job-status-card">
            <div className="job-status-header">
              <span className="job-status-pill">{job.status}</span>
              <span className="job-progress-percent">{job.progress}%</span>
            </div>
            <progress className="job-progress-bar" value={job.progress} max="100" />
            <p className="muted">실제 분석 기간 (UTC): {job.periodStart} ~ {job.periodEnd} · 종료 시각 제외 · main</p>
            <p className="job-status-desc muted">
              {job.status === "QUEUED" && "작업 대기열에 등록되었습니다..."}
              {job.status === "RUNNING" && "GitHub 활동 데이터를 수집 및 분석 중입니다..."}
              {job.status === "COMPLETED" && "기여 분석이 완료되었습니다!"}
              {job.status === "FAILED" && (job.errorMessage || "분석 작업 중 오류가 발생했습니다.")}
            </p>
          </div>
        )}

        {error && <p className="error-message">{error}</p>}

        {analysisId && (
          <div className="analysis-success-box">
            <p>기여 분석이 성공적으로 완료되었습니다!</p>
            <Link
              className="button primary"
              href={`/repositories/${repositoryId}/analysis/${analysisId}`}
            >
              분석 결과 확인하기 &rarr;
            </Link>
          </div>
        )}
      </div>
    </div>
  );
}
