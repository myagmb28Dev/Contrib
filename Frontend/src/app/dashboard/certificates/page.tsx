import { AppNav } from "@/components/app-nav";
import { CertificatesClient } from "@/components/certificates-client";

export default function CertificatesPage() {
  return <main className="shell"><p className="eyebrow">Contribution Attestation</p><AppNav />
    <div className="card stack"><h1>Certificates</h1><CertificatesClient /></div></main>;
}
