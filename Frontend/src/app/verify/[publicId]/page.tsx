import { VerificationClient } from "@/components/verification-client";

export default async function VerifyPage({ params }: { params: Promise<{ publicId: string }> }) {
  const { publicId } = await params;
  return <main className="shell"><p className="eyebrow">Public Verification</p><div className="card stack">
    <h1>Certificate Verification</h1><VerificationClient publicId={publicId} /></div></main>;
}
