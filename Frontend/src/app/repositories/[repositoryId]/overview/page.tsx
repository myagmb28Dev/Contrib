import { RepositoryOverviewClient } from "@/components/repository-overview-client";

export default async function RepositoryOverviewPage({ params }: { params: Promise<{ repositoryId: string }> }) {
  const { repositoryId } = await params;
  return <main className="shell"><p className="eyebrow">Repository</p><div className="card stack">
    <h1>Repository Overview</h1><RepositoryOverviewClient repositoryId={repositoryId} /></div></main>;
}
