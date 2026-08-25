"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { getRepository, getRepositoryAnalyses, type Analysis, type Repository } from "@/lib/api";

export function RepositoryOverviewClient({ repositoryId }: { repositoryId: string }) {
  const [repository, setRepository] = useState<Repository | null>(null);
  const [analyses, setAnalyses] = useState<Analysis[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([getRepository(repositoryId), getRepositoryAnalyses(repositoryId)])
      .then(([repo, values]) => { setRepository(repo); setAnalyses(values); })
      .catch((reason: Error) => setError(reason.message));
  }, [repositoryId]);

  if (error) return <p className="error-message">{error}</p>;
  if (!repository) return <p className="muted">저장소를 불러오는 중입니다...</p>;
  return (
    <div className="stack full-width">
      <dl className="identity-list">
        <div><dt>저장소</dt><dd>{repository.fullName}</dd></div>
        <div><dt>기본 브랜치</dt><dd>{repository.defaultBranch}</dd></div>
        <div><dt>언어</dt><dd>{repository.language ?? "미지정"}</dd></div>
      </dl>
      <div className="actions">
        <Link className="button primary" href={`/repositories/${repositoryId}/analyze`}>새 분석</Link>
        <a className="button" href={repository.url} target="_blank" rel="noreferrer">GitHub 열기</a>
      </div>
      <h2>완료된 분석</h2>
      <div className="item-list">
        {analyses.map((analysis) => (
          <Link className="list-card" key={analysis.id}
            href={`/repositories/${repositoryId}/analysis/${analysis.id}`}>
            <strong>{analysis.score}점</strong>
            <span>{new Date(analysis.periodStart).toLocaleDateString()} ~ {new Date(analysis.periodEnd).toLocaleDateString()}</span>
          </Link>
        ))}
      </div>
    </div>
  );
}
