"use client";

import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";

import { createAnalysis, getAnalysisJob, getRepositoryAnalyses, type AnalysisJob } from "@/lib/api";

export function AnalyzeClient({ repositoryId }: { repositoryId: string }) {
  const today = new Date();
  const monthAgo = new Date(today); monthAgo.setMonth(today.getMonth() - 1);
  const [start, setStart] = useState(monthAgo.toISOString().slice(0, 10));
  const [end, setEnd] = useState(today.toISOString().slice(0, 10));
  const [job, setJob] = useState<AnalysisJob | null>(null);
  const [analysisId, setAnalysisId] = useState<string | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!job || ["COMPLETED", "FAILED", "CANCELLED"].includes(job.status)) return;
    const timer = window.setInterval(async () => {
      const next = await getAnalysisJob(job.id);
      setJob(next);
      if (next.status === "COMPLETED") {
        const analyses = await getRepositoryAnalyses(repositoryId);
        setAnalysisId(analyses.find((analysis) => analysis.jobId === next.id)?.id ?? null);
      }
    }, 1500);
    return () => window.clearInterval(timer);
  }, [job, repositoryId]);

  async function submit(event: FormEvent) {
    event.preventDefault(); setError(""); setAnalysisId(null);
    try {
      const created = await createAnalysis(repositoryId,
        new Date(`${start}T00:00:00Z`).toISOString(), new Date(`${end}T23:59:59.999Z`).toISOString());
      setJob(created);
      if (created.status === "COMPLETED") {
        const analyses = await getRepositoryAnalyses(repositoryId);
        setAnalysisId(analyses.find((analysis) => analysis.jobId === created.id)?.id ?? null);
      }
    } catch (reason) { setError(reason instanceof Error ? reason.message : "분석을 시작하지 못했습니다."); }
  }

  return (
    <div className="stack full-width">
      <form className="form-grid" onSubmit={submit}>
        <label>시작일<input type="date" value={start} onChange={(event) => setStart(event.target.value)} required /></label>
        <label>종료일<input type="date" value={end} onChange={(event) => setEnd(event.target.value)} required /></label>
        <button className="button primary" type="submit" disabled={!!job && !["COMPLETED", "FAILED"].includes(job.status)}>분석 시작</button>
      </form>
      {job && <div className="status-box"><strong>{job.status}</strong><progress value={job.progress} max="100" /> <span>{job.progress}%</span></div>}
      {job?.errorMessage && <p className="error-message">{job.errorMessage}</p>}
      {error && <p className="error-message">{error}</p>}
      {analysisId && <Link className="button primary" href={`/repositories/${repositoryId}/analysis/${analysisId}`}>결과 보기</Link>}
    </div>
  );
}
