import { AppHeader } from "@/components/app-header";
import { AnalyzeClient } from "@/components/analyze-client";

export default async function RepositoryAnalyzePage({
  params,
}: {
  params: Promise<{ repositoryId: string }>;
}) {
  const { repositoryId } = await params;
  return (
    <>
      <AppHeader />
      <main className="shell narrow-shell">
        <AnalyzeClient repositoryId={repositoryId} />
      </main>
    </>
  );
}
