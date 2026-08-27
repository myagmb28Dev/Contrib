"use client";

import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";

import { Breadcrumb } from "./breadcrumb";
import {
  createAnalysis,
  getAnalysisJob,
  getRepository,
  getRepositoryAnalyses,
  type AnalysisJob,
  type Repository,
} from "@/lib/api";

export function AnalyzeClient({ repositoryId }: { repositoryId: string }) {
  const today = new Date();
  const monthAgo = new Date(today);
  monthAgo.setMonth(today.getMonth() - 1);

  const [repository, setRepository] = useState<Repository | null>(null);
  const [start, setStart] = useState(monthAgo.toISOString().slice(0, 10));
  const [end, setEnd] = useState(today.toISOString().slice(0, 10));
  const [job, setJob] = useState<AnalysisJob | null>(null);
  const [analysisId, setAnalysisId] = useState<string | null>(null);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getRepository(repositoryId)
      .then(setRepository)
      .catch(() => {});
  }, [repositoryId]);

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

  function setPreset(months: number) {
    const endD = new Date();
    const startD = new Date();
    startD.setMonth(endD.getMonth() - months);
    setStart(startD.toISOString().slice(0, 10));
    setEnd(endD.toISOString().slice(0, 10));
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError("");
    setAnalysisId(null);
    setSubmitting(true);
    try {
      const created = await createAnalysis(
        repositoryId,
        new Date(`${start}T00:00:00Z`).toISOString(),
        new Date(`${end}T23:59:59.999Z`).toISOString()
      );
      setJob(created);
      if (created.status === "COMPLETED") {
        const analyses = await getRepositoryAnalyses(repositoryId);
        setAnalysisId(analyses.find((analysis) => analysis.jobId === created.id)?.id ?? null);
      }
    } catch (reason) {
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
            분석할 활동 기간(UTC 기준)을 선택하면 GitHub 활동을 수집하고 Gemini AI가 기여도를 요약합니다.
          </p>
        </div>

        {/* Preset Range Buttons */}
        <div className="preset-row">
          <span className="preset-label">빠른 기간 선택:</span>
          <button type="button" className="preset-btn" onClick={() => setPreset(1)}>
            최근 1개월
          </button>
          <button type="button" className="preset-btn" onClick={() => setPreset(3)}>
            최근 3개월
          </button>
          <button type="button" className="preset-btn" onClick={() => setPreset(6)}>
            최근 6개월
          </button>
          <button type="button" className="preset-btn" onClick={() => setPreset(12)}>
            최근 1년
          </button>
        </div>

        <form className="form-grid" onSubmit={submit}>
          <div className="date-inputs-row">
            <label>
              시작일 (Start Date)
              <input
                type="date"
                value={start}
                onChange={(event) => setStart(event.target.value)}
                required
                disabled={isWorking}
              />
            </label>
            <label>
              종료일 (End Date)
              <input
                type="date"
                value={end}
                onChange={(event) => setEnd(event.target.value)}
                required
                disabled={isWorking}
              />
            </label>
          </div>

          <div className="analysis-info-box">
            <span className="info-icon">💡</span>
            <div>
              <strong>수집 및 분석 지표</strong>
              <p>
                커밋(수정/추가 라인 수), 풀 리퀘스트 생성 및 병합, 코드 리뷰 제출, 활동 일수를
                포괄하여 Contribution Score와 Google Gemini 2.0 Flash AI 요약을 생성합니다.
              </p>
            </div>
          </div>

          <button
            className="button primary hero-action-btn"
            type="submit"
            disabled={isWorking}
          >
            {isWorking ? "분석 작업 진행 중..." : "🚀 기여 분석 시작하기"}
          </button>
        </form>

        {job && (
          <div className="job-status-card">
            <div className="job-status-header">
              <span className="job-status-pill">{job.status}</span>
              <span className="job-progress-percent">{job.progress}%</span>
            </div>
            <progress className="job-progress-bar" value={job.progress} max="100" />
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
