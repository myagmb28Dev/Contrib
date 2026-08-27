import { AppHeader } from "@/components/app-header";
import { CertificatesClient } from "@/components/certificates-client";

export default function CertificatesPage() {
  return (
    <>
      <AppHeader />
      <main className="shell">
        <div className="page-header">
          <div>
            <p className="eyebrow">CONTRIBUTION ATTESTATION</p>
            <h1>인증서 목록</h1>
            <p className="muted">
              Base Sepolia 블록체인에 Self-Attest된 Contribution Certificate 목록입니다.
            </p>
          </div>
        </div>
        <CertificatesClient />
      </main>
    </>
  );
}
