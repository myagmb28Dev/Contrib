import { redirect } from "next/navigation";

export default async function RepositoryPage({ params }: { params: Promise<{ repositoryId: string }> }) {
  const { repositoryId } = await params;
  redirect(`/repositories/${repositoryId}/overview`);
}
