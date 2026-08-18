"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { getCertificates, type Certificate } from "@/lib/api";

export function CertificatesClient() {
  const [items, setItems] = useState<Certificate[]>([]);
  const [error, setError] = useState("");
  useEffect(() => { getCertificates().then(setItems).catch((reason: Error) => setError(reason.message)); }, []);
  return <div className="item-list full-width">
    {error && <p className="error-message">{error}</p>}
    {items.map((certificate) => <Link className="list-card" key={certificate.id} href={`/certificates/${certificate.id}`}>
      <strong>{certificate.status}</strong><span className="monospace">{certificate.hash}</span>
    </Link>)}
    {!error && items.length === 0 && <p className="muted">아직 발급한 인증서가 없어.</p>}
  </div>;
}
