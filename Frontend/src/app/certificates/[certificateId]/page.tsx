import { CertificateDetailClient } from "@/components/certificate-detail-client";

export default async function CertificatePage({ params }: { params: Promise<{ certificateId: string }> }) {
  const { certificateId } = await params;
  return <main className="shell"><p className="eyebrow">Certificate</p><div className="card stack">
    <h1>Certificate</h1><CertificateDetailClient certificateId={certificateId} /></div></main>;
}
