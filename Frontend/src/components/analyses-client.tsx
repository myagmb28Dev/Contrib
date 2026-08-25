"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { getAnalyses, type Analysis } from "@/lib/api";

export function AnalysesClient() {
  const [items, setItems] = useState<Analysis[]>([]);
  const [error, setError] = useState("");
  useEffect(() => { getAnalyses().then(setItems).catch((reason: Error) => setError(reason.message)); }, []);
  return <div className="item-list full-width">
    {error && <p className="error-message">{error}</p>}
    {items.map((analysis) => <Link className="list-card" key={analysis.id}
      href={`/repositories/${analysis.repositoryId}/analysis/${analysis.id}`}>
      <strong>{analysis.score}점 · {analysis.scoreVersion}</strong>
      <span>{new Date(analysis.periodStart).toLocaleDateString()} ~ {new Date(analysis.periodEnd).toLocaleDateString()}</span>
    </Link>)}
    {!error && items.length === 0 && <p className="muted">아직 완료된 분석이 없습니다.</p>}
  </div>;
}
