import { AnalysisDetailClient } from "@/components/analysis-detail-client";

export default async function AnalysisPage({ params }: { params: Promise<{ analysisId: string }> }) {
  const { analysisId } = await params;
  return <main className="shell"><p className="eyebrow">Analysis</p><div className="card stack">
    <h1>Analysis Result</h1><AnalysisDetailClient analysisId={analysisId} /></div></main>;
}
