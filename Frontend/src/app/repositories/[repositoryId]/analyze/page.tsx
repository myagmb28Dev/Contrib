import { AnalyzeClient } from "@/components/analyze-client";

export default async function RepositoryAnalyzePage({ params }: { params: Promise<{ repositoryId: string }> }) {
  const { repositoryId } = await params;
  return <main className="shell narrow-shell"><p className="eyebrow">Analysis</p><div className="card stack">
    <h1>Analyze Repository</h1><p className="muted">UTC 기준 기간을 선택하면 GitHub 활동을 수집합니다.</p>
    <AnalyzeClient repositoryId={repositoryId} /></div></main>;
}
