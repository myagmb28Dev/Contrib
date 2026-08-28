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
              발급된 공식 기여 인증서 및 온체인 증명 목록입니다.
            </p>
          </div>
        </div>
        <CertificatesClient />
      </main>
    </>
  );
}
