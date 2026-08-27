import { AppHeader } from "@/components/app-header";
import { RepositoryOverviewClient } from "@/components/repository-overview-client";

export default async function RepositoryOverviewPage({
  params,
}: {
  params: Promise<{ repositoryId: string }>;
}) {
  const { repositoryId } = await params;
  return (
    <>
      <AppHeader />
      <main className="shell">
        <RepositoryOverviewClient repositoryId={repositoryId} />
      </main>
    </>
  );
}
