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

function formatPeriodDate(isoString: string): string {
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) {
    return isoString;
  }
  const year = date.getUTCFullYear();
  const month = String(date.getUTCMonth() + 1).padStart(2, "0");
  const day = String(date.getUTCDate()).padStart(2, "0");
  return `${year}.${month}.${day}`;
}

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
            main 브랜치의 생성일부터 현재까지의 활동을 분석합니다.
          </p>
        </div>

        <div className="analysis-info-box">
          <div>
            <strong>분석 기간</strong>
            <p role="status">{repository?.githubCreatedAt
              ? `${formatPeriodDate(repository.githubCreatedAt)} ~ 현재 (저장소 생성일 기준)`
              : "분석 시작 시 GitHub에서 실제 저장소 생성 시각을 자동으로 확인합니다."}</p>
          </div>
        </div>

        <form className="form-grid" onSubmit={submit}>
          <div className="analysis-info-box">
            <div>
              <strong>수집 지표</strong>
              <p>
                커밋 이력, PR 생성 및 병합, 코드 리뷰, 활동 일수를 종합 분석합니다.
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
