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
  getBenchmarks,
  type BenchmarkSummary,
  type AnalysisJob,
  type Repository,
} from "@/lib/api";

function monthsAgoUtc(now: Date, months: number): string {
  const result = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth() - months, 1));
  const lastDay = new Date(Date.UTC(result.getUTCFullYear(), result.getUTCMonth() + 1, 0)).getUTCDate();
  result.setUTCDate(Math.min(now.getUTCDate(), lastDay));
  return result.toISOString().slice(0, 10);
}

export function AnalyzeClient({ repositoryId }: { repositoryId: string }) {
  const router = useRouter();
  const today = new Date();

  const [repository, setRepository] = useState<Repository | null>(null);
  const [allTime, setAllTime] = useState(false);

  const [start, setStart] = useState(monthsAgoUtc(today, 1));
  const [end, setEnd] = useState(today.toISOString().slice(0, 10));
  const [job, setJob] = useState<AnalysisJob | null>(null);
  const [analysisId, setAnalysisId] = useState<string | null>(null);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [benchmarks, setBenchmarks] = useState<BenchmarkSummary[]>([]);
  useEffect(() => { getBenchmarks().then(setBenchmarks).catch(() => setBenchmarks([])); }, []);

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

  function setPreset(months: number) {
    const endD = new Date();
    setAllTime(false);
    setStart(monthsAgoUtc(endD, months));
    setEnd(endD.toISOString().slice(0, 10));
  }

  function setAllTimePreset() {
    setAllTime(true);
    setStart(repository?.githubCreatedAt?.slice(0, 10) ?? "");
    setEnd(new Date().toISOString().slice(0, 10));
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError("");
    setAnalysisId(null);
    setSubmitting(true);
    try {
      const created = await createAnalysis(repositoryId, allTime ? { allTime: true } : {
        periodStart: new Date(`${start}T00:00:00Z`).toISOString(),
        periodEnd: new Date(Date.parse(`${end}T00:00:00Z`) + 86400000).toISOString(),
      });
      setJob(created);
      if (allTime) {
        setStart(created.periodStart.slice(0, 10));
        setEnd(created.periodEnd.slice(0, 10));
        setRepository(previous => previous ? { ...previous, githubCreatedAt: created.periodStart } : previous);
      }
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
            main 브랜치를 기준으로 분석합니다. 기간을 지정하거나 저장소 생성일부터 현재까지 전체 기간을 선택할 수 있습니다.
          </p>
        </div>

        {/* Preset Range Buttons */}
        <div className="stack">
          <p>백분위는 main이 기본 브랜치인 공개 저장소를 분석하고, 같은 기간·언어·활동 규모의 비교 집단이 있을 때 제공됩니다.
            비교 표본이 부족하면 원시 활동 지표만 표시합니다.</p>
          {benchmarks.length > 0 && <label>공개 비교 데이터가 있는 기간
            <select aria-label="공개 비교 기간" disabled={isWorking} value={allTime ? "" : benchmarks.find(b => b.periodStart.slice(0, 10) === start
              && new Date(Date.parse(b.periodEnd) - 86400000).toISOString().slice(0, 10) === end)?.id ?? ""} onChange={(event) => {
              const selected = benchmarks.find(b => b.id === event.target.value);
              if (!selected) return;
              setAllTime(false);
              setStart(selected.periodStart.slice(0, 10));
              setEnd(new Date(Date.parse(selected.periodEnd) - 86400000).toISOString().slice(0, 10));
            }}>
              <option value="">직접 기간 선택</option>
              {benchmarks.map(b => <option key={b.id} value={b.id}>
                {b.periodStart.slice(0, 10)} ~ {new Date(Date.parse(b.periodEnd) - 86400000).toISOString().slice(0, 10)} · {b.languages.join(", ")} · {b.repositoryCount}개 저장소
              </option>)}
            </select>
          </label>}
        </div>
        <div className="preset-row">
          <span className="preset-label">빠른 기간 선택:</span>
          <button type="button" className="preset-btn" disabled={isWorking} onClick={() => setPreset(1)}>
            최근 1개월
          </button>
          <button type="button" className="preset-btn" disabled={isWorking} onClick={() => setPreset(3)}>
            최근 3개월
          </button>
          <button type="button" className="preset-btn" disabled={isWorking} onClick={() => setPreset(6)}>
            최근 6개월
          </button>
          <button type="button" className="preset-btn" disabled={isWorking} onClick={() => setPreset(12)}>
            최근 1년
          </button>
          <button type="button" className="preset-btn highlight" onClick={setAllTimePreset} disabled={isWorking || !repository} aria-pressed={allTime}>
            전체 기간
          </button>
          {allTime && <button type="button" className="preset-btn" disabled={isWorking} onClick={() => {
            setAllTime(false);
            if (!start) setStart(monthsAgoUtc(new Date(), 1));
          }}>날짜 직접 지정</button>}
        </div>

        {allTime && <p role="status">전체 기간: {repository?.githubCreatedAt
          ? `${repository.githubCreatedAt}부터 분석 요청 시점까지`
          : "분석 시작 시 GitHub에서 실제 저장소 생성 시각을 확인합니다."}
          <br />서버에서 생성 시각과 현재 시각을 확정합니다. 1년·5년으로 제한하지 않습니다.</p>}

        <form className="form-grid" onSubmit={submit}>
          <div className="date-inputs-row" style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: "12px" }}>
            <label>
              시작일 (Start Date)
              <input
                type="date"
                value={start}
                onChange={(event) => setStart(event.target.value)}
                required={!allTime}
                disabled={isWorking || allTime}
              />
            </label>
            <label>
              종료일 (End Date)
              <input
                type="date"
                value={end}
                onChange={(event) => setEnd(event.target.value)}
                required={!allTime}
                disabled={isWorking || allTime}
              />
            </label>
          </div>

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
