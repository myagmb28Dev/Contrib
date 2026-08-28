import { AppHeader } from "@/components/app-header";
import { CertificateDetailClient } from "@/components/certificate-detail-client";

export default async function CertificatePage({
  params,
}: {
  params: Promise<{ certificateId: string }>;
}) {
  const { certificateId } = await params;
  return (
    <>
      <AppHeader />
      <main className="shell">
        <CertificateDetailClient certificateId={certificateId} />
      </main>
    </>
  );
}
