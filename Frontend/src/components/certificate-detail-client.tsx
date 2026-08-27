"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { createWalletClient, custom, type EIP1193Provider } from "viem";

import { Breadcrumb } from "./breadcrumb";
import {
  ApiRequestError,
  getAttestationIntent,
  getCertificate,
  getCertificateAttestation,
  getRevocationIntent,
  submitAttestation,
  submitRevocation,
  type Attestation,
  type AttestationIntent,
  type Certificate,
} from "@/lib/api";

const attestationAbi = [
  {
    type: "function",
    name: "issue",
    stateMutability: "nonpayable",
    inputs: [
      { name: "certificateId", type: "bytes32" },
      { name: "certificateHash", type: "bytes32" },
      { name: "subject", type: "address" },
    ],
    outputs: [],
  },
  {
    type: "function",
    name: "revoke",
    stateMutability: "nonpayable",
    inputs: [{ name: "certificateId", type: "bytes32" }],
    outputs: [],
  },
] as const;

declare global {
  interface Window {
    ethereum?: EIP1193Provider;
  }
}

export function CertificateDetailClient({ certificateId }: { certificateId: string }) {
  const [certificate, setCertificate] = useState<Certificate | null>(null);
  const [attestation, setAttestation] = useState<Attestation | null>(null);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState("");
  const [revocationReason, setRevocationReason] = useState("");
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  useEffect(() => {
    getCertificate(certificateId)
      .then(setCertificate)
      .catch((reason: Error) => setError(reason.message));

    getCertificateAttestation(certificateId)
      .then(setAttestation)
      .catch((reason: Error) => {
        if (!(reason instanceof ApiRequestError) || reason.status !== 404) {
          setError(reason.message);
        }
      });
  }, [certificateId]);

  useEffect(() => {
    if (
      !attestation ||
      (attestation.status !== "PENDING" && attestation.revocationStatus !== "PENDING")
    )
      return;

    const timer = window.setInterval(async () => {
      try {
        const next = await getCertificateAttestation(certificateId);
        setAttestation(next);
        if (next.status === "CONFIRMED" || next.revocationStatus === "CONFIRMED") {
          setCertificate(await getCertificate(certificateId));
        }
      } catch (reason) {
        setError(reason instanceof Error ? reason.message : "트랜잭션 상태를 확인하지 못했습니다.");
      }
    }, 1500);

    return () => window.clearInterval(timer);
  }, [attestation, certificateId]);

  function copyText(text: string, key: string) {
    navigator.clipboard.writeText(text).then(() => {
      setCopiedKey(key);
      setTimeout(() => setCopiedKey(null), 2000);
    });
  }

  async function connectWallet(intent: AttestationIntent) {
    if (!window.ethereum) throw new Error("MetaMask 등 브라우저 Web3 지갑을 찾을 수 없습니다.");
    const wallet = createWalletClient({ transport: custom(window.ethereum) });
    const [account] = await wallet.requestAddresses();
    const chainId = `0x${intent.chainId.toString(16)}`;
    try {
      await window.ethereum.request({
        method: "wallet_switchEthereumChain",
        params: [{ chainId }],
      });
    } catch (switchError) {
      if ((switchError as { code?: number }).code !== 4902 || intent.chainId !== 84532)
        throw switchError;
      await window.ethereum.request({
        method: "wallet_addEthereumChain",
        params: [
          {
            chainId,
            chainName: "Base Sepolia",
            nativeCurrency: { name: "Ether", symbol: "ETH", decimals: 18 },
            rpcUrls: ["https://sepolia.base.org"],
            blockExplorerUrls: ["https://sepolia.basescan.org"],
          },
        ],
      });
    }
    return { wallet, account };
  }

  async function publish() {
    setWorking(true);
    setError("");
    try {
      const intent = await getAttestationIntent(certificateId);
      const { wallet, account } = await connectWallet(intent);
      const hash = await wallet.writeContract({
        account,
        chain: null,
        address: intent.contractAddress,
        abi: attestationAbi,
        functionName: "issue",
        args: intent.arguments as [`0x${string}`, `0x${string}`, `0x${string}`],
      });
      setAttestation(await submitAttestation(certificateId, hash, account));
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "온체인 발급에 실패했습니다.");
    } finally {
      setWorking(false);
    }
  }

  async function revoke() {
    if (!revocationReason.trim()) {
      setError("폐기 사유를 입력해 주세요.");
      return;
    }
    setWorking(true);
    setError("");
    try {
      const intent = await getRevocationIntent(certificateId);
      const { wallet, account } = await connectWallet(intent);
      const hash = await wallet.writeContract({
        account,
        chain: null,
        address: intent.contractAddress,
        abi: attestationAbi,
        functionName: "revoke",
        args: [intent.arguments[0]],
      });
      setAttestation(await submitRevocation(certificateId, hash, account, revocationReason.trim()));
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "온체인 폐기에 실패했습니다.");
    } finally {
      setWorking(false);
    }
  }

  if (!certificate) {
    return (
      <div className="card loading-card">
        <div className="skeleton-line lg" />
        <div className="skeleton-line md" />
        <p className={error ? "error-message" : "muted"}>
          {error || "인증서 정보를 불러오는 중입니다..."}
        </p>
      </div>
    );
  }

  const payload = certificate.payload as Record<string, unknown> | undefined;
  const score = payload?.score ? String(payload.score) : "80";
  const repoName = payload?.repository ? String(payload.repository) : "GitHub Repository";

  return (
    <div className="stack full-width">
      <Breadcrumb
        items={[
          { label: "대시보드", href: "/dashboard" },
          { label: "인증서 목록", href: "/dashboard/certificates" },
          { label: "인증서 상세" },
        ]}
      />

      {/* Web3 Certificate Showcase Card */}
      <section className="certificate-stage full-width" aria-label="Contribution Certificate">
        <div className="certificate-glow" />
        <article className="certificate-preview standalone">
          <header className="certificate-heading">
            <div>
              <span className="preview-label">CONTRIBUTION CERTIFICATE</span>
              <strong>Verified GitHub Contribution</strong>
            </div>
            <span
              className={`verified-badge ${
                certificate.status === "REVOKED" ? "status-revoked" : "status-valid"
              }`}
            >
              {certificate.status === "REVOKED" ? "✕ REVOKED" : "✓ " + certificate.status}
            </span>
          </header>

          <div className="certificate-repository">
            <span>Target Repository</span>
            <strong>{repoName}</strong>
          </div>

          <div className="certificate-score-row">
            <div className="preview-score">
              <span>Contribution score</span>
              <strong>{score}</strong>
              <small>/ 100</small>
            </div>
            <div className="chain-badge-box">
              <span className="network-pill">Base Sepolia</span>
            </div>
          </div>

          <footer className="certificate-footer">
            <div className="hash-box">
              <span>Keccak-256 Hash:</span>
              <code className="monospace">{certificate.hash}</code>
              <button
                type="button"
                className="copy-btn"
                onClick={() => copyText(certificate.hash, "hash")}
              >
                {copiedKey === "hash" ? "복사됨!" : "복사"}
              </button>
            </div>
          </footer>
        </article>
      </section>

      {/* Attestation Details & Action Section */}
      <section className="card full-width">
        <div className="card-header-simple">
          <h3>인증서 상세 메타데이터</h3>
          <p className="muted">온체인 증명 상태 및 공개 검증 링크 정보입니다.</p>
        </div>

        <dl className="identity-list">
          <div>
            <dt>발급 상태</dt>
            <dd>
              <strong>{certificate.status}</strong>
            </dd>
          </div>
          <div>
            <dt>Subject 지갑 주소</dt>
            <dd className="monospace">{certificate.subjectWalletAddress ?? "미설정 (오프체인)"}</dd>
          </div>
          <div>
            <dt>공개 검증 ID</dt>
            <dd className="monospace">
              <span>{certificate.publicId}</span>
              <button
                type="button"
                className="copy-btn ml"
                onClick={() => copyText(certificate.publicId, "publicId")}
              >
                {copiedKey === "publicId" ? "복사됨!" : "ID 복사"}
              </button>
            </dd>
          </div>
          <div>
            <dt>발급 일시</dt>
            <dd>{new Date(certificate.issuedAt).toLocaleString()}</dd>
          </div>
          {attestation && (
            <div>
              <dt>온체인 트랜잭션</dt>
              <dd className="monospace">
                <a
                  href={`https://sepolia.basescan.org/tx/${attestation.transactionHash}`}
                  target="_blank"
                  rel="noreferrer"
                  className="tx-link"
                >
                  {attestation.transactionHash} ↗
                </a>{" "}
                ({attestation.status})
              </dd>
            </div>
          )}
        </dl>

        <div className="actions mt">
          <button
            className="button primary"
            onClick={publish}
            disabled={working || !certificate.subjectWalletAddress || certificate.status === "REVOKED"}
          >
            {working ? "지갑 서명 진행 중..." : "🚀 Base Sepolia 온체인 발급"}
          </button>
          <Link className="button" href={`/verify/${certificate.publicId}`}>
            🔍 공개 검증 화면 열기
          </Link>
        </div>

        {error && <p className="error-message mt">{error}</p>}
      </section>

      {/* Revocation Section */}
      {attestation?.status === "CONFIRMED" && certificate.status !== "REVOKED" && (
        <section className="card full-width danger-card">
          <h4>인증서 온체인 폐기 (Revocation)</h4>
          <p className="muted">
            인증서를 폐기하면 블록체인에 폐기 사유가 영구 기록되며 검증 시 REVOKED 상태로 표시됩니다.
          </p>
          <div className="form-grid">
            <label>
              폐기 사유 입력
              <input
                value={revocationReason}
                onChange={(event) => setRevocationReason(event.target.value)}
                placeholder="예: 기여 내역 재조정 또는 지갑 변경"
                maxLength={1000}
              />
            </label>
            <button
              className="button danger-outline"
              onClick={revoke}
              disabled={working || attestation.revocationStatus === "PENDING"}
            >
              {attestation.revocationStatus === "PENDING" ? "폐기 확인 중..." : "⚠️ 온체인 폐기 실행"}
            </button>
          </div>
        </section>
      )}

      {/* Canonical Payload Viewer */}
      <details className="full-width card">
        <summary>Canonical Payload (JSON)</summary>
        <pre>{JSON.stringify(certificate.payload, null, 2)}</pre>
      </details>
    </div>
  );
}
