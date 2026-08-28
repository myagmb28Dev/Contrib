import { AppHeader } from "@/components/app-header";
import { AnalysisDetailClient } from "@/components/analysis-detail-client";

export default async function AnalysisPage({
  params,
}: {
  params: Promise<{ analysisId: string }>;
}) {
  const { analysisId } = await params;
  return (
    <>
      <AppHeader />
      <main className="shell">
        <AnalysisDetailClient analysisId={analysisId} />
      </main>
    </>
  );
}
