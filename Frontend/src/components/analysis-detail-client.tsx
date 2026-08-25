"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { createCertificate, getAnalysis, type Analysis } from "@/lib/api";

export function AnalysisDetailClient({ analysisId }: { analysisId: string }) {
  const [analysis, setAnalysis] = useState<Analysis | null>(null);
  const [wallet, setWallet] = useState("");
  const [certificateId, setCertificateId] = useState<string | null>(null);
  const [error, setError] = useState("");
  useEffect(() => { getAnalysis(analysisId).then(setAnalysis).catch((reason: Error) => setError(reason.message)); }, [analysisId]);
  async function issue() {
    try { setCertificateId((await createCertificate(analysisId, wallet || null)).id); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "인증서를 생성하지 못했습니다."); }
  }
  if (error && !analysis) return <p className="error-message">{error}</p>;
  if (!analysis) return <p className="muted">분석 결과를 불러오는 중입니다...</p>;
  return (
    <div className="stack full-width">
      <div className="score">{analysis.score}<span>/ 100</span></div>
      <p>{analysis.summary}</p>
      <div className="metric-grid">{Object.entries(analysis.metrics).map(([key, value]) => <div key={key}><span>{key}</span><strong>{value}</strong></div>)}</div>
      <label className="full-width">Subject 지갑 주소 (온체인 발급 시 필수)<input value={wallet} onChange={(event) => setWallet(event.target.value)} placeholder="0x..." /></label>
      <button className="button primary" onClick={issue}>인증서 만들기</button>
      {error && <p className="error-message">{error}</p>}
      {certificateId && <Link className="button" href={`/certificates/${certificateId}`}>인증서 열기</Link>}
    </div>
  );
}
