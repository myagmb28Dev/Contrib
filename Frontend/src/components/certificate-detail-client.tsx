"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { createWalletClient, custom, type EIP1193Provider } from "viem";

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

const attestationAbi = [{
  type: "function", name: "issue", stateMutability: "nonpayable",
  inputs: [{ name: "certificateId", type: "bytes32" }, { name: "certificateHash", type: "bytes32" }, { name: "subject", type: "address" }],
  outputs: [],
}, {
  type: "function", name: "revoke", stateMutability: "nonpayable",
  inputs: [{ name: "certificateId", type: "bytes32" }], outputs: [],
}] as const;

declare global { interface Window { ethereum?: EIP1193Provider } }

export function CertificateDetailClient({ certificateId }: { certificateId: string }) {
  const [certificate, setCertificate] = useState<Certificate | null>(null);
  const [attestation, setAttestation] = useState<Attestation | null>(null);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState("");
  const [revocationReason, setRevocationReason] = useState("");

  useEffect(() => {
    getCertificate(certificateId).then(setCertificate).catch((reason: Error) => setError(reason.message));
    getCertificateAttestation(certificateId).then(setAttestation).catch((reason: Error) => {
      if (!(reason instanceof ApiRequestError) || reason.status !== 404) setError(reason.message);
    });
  }, [certificateId]);

  useEffect(() => {
    if (!attestation || (attestation.status !== "PENDING" && attestation.revocationStatus !== "PENDING")) return;
    const timer = window.setInterval(async () => {
      try {
        const next = await getCertificateAttestation(certificateId);
        setAttestation(next);
        if (next.status === "CONFIRMED" || next.revocationStatus === "CONFIRMED") {
          setCertificate(await getCertificate(certificateId));
        }
      } catch (reason) {
        setError(reason instanceof Error ? reason.message : "트랜잭션 상태를 확인하지 못했어.");
      }
    }, 1500);
    return () => window.clearInterval(timer);
  }, [attestation, certificateId]);

  async function connectWallet(intent: AttestationIntent) {
    if (!window.ethereum) throw new Error("브라우저 지갑을 찾지 못했어.");
    const wallet = createWalletClient({ transport: custom(window.ethereum) });
    const [account] = await wallet.requestAddresses();
    const chainId = `0x${intent.chainId.toString(16)}`;
    try {
      await window.ethereum.request({ method: "wallet_switchEthereumChain", params: [{ chainId }] });
    } catch (switchError) {
      if ((switchError as { code?: number }).code !== 4902 || intent.chainId !== 84532) throw switchError;
      await window.ethereum.request({ method: "wallet_addEthereumChain", params: [{
        chainId,
        chainName: "Base Sepolia",
        nativeCurrency: { name: "Ether", symbol: "ETH", decimals: 18 },
        rpcUrls: ["https://sepolia.base.org"],
        blockExplorerUrls: ["https://sepolia.basescan.org"],
      }] });
    }
    return { wallet, account };
  }

  async function publish() {
    setWorking(true); setError("");
    try {
      const intent = await getAttestationIntent(certificateId);
      const { wallet, account } = await connectWallet(intent);
      const hash = await wallet.writeContract({ account, chain: null, address: intent.contractAddress, abi: attestationAbi,
        functionName: "issue", args: intent.arguments as [`0x${string}`, `0x${string}`, `0x${string}`] });
      setAttestation(await submitAttestation(certificateId, hash, account));
    } catch (reason) { setError(reason instanceof Error ? reason.message : "온체인 발급에 실패했어."); }
    finally { setWorking(false); }
  }

  async function revoke() {
    if (!revocationReason.trim()) { setError("폐기 사유를 입력해줘."); return; }
    setWorking(true); setError("");
    try {
      const intent = await getRevocationIntent(certificateId);
      const { wallet, account } = await connectWallet(intent);
      const hash = await wallet.writeContract({ account, chain: null, address: intent.contractAddress,
        abi: attestationAbi, functionName: "revoke", args: [intent.arguments[0]] });
      setAttestation(await submitRevocation(certificateId, hash, account, revocationReason.trim()));
    } catch (reason) { setError(reason instanceof Error ? reason.message : "온체인 폐기에 실패했어."); }
    finally { setWorking(false); }
  }

  if (!certificate) return <p className={error ? "error-message" : "muted"}>{error || "인증서를 불러오는 중..."}</p>;
  return <div className="stack full-width">
    <dl className="identity-list">
      <div><dt>상태</dt><dd>{certificate.status}</dd></div>
      <div><dt>Hash</dt><dd className="monospace">{certificate.hash}</dd></div>
      <div><dt>Subject Wallet</dt><dd className="monospace">{certificate.subjectWalletAddress ?? "미설정"}</dd></div>
      <div><dt>Public ID</dt><dd className="monospace">{certificate.publicId}</dd></div>
    </dl>
    <div className="actions"><button className="button primary" onClick={publish} disabled={working || !certificate.subjectWalletAddress}>{working ? "지갑 확인 중..." : "온체인 발급"}</button>
      <Link className="button" href={`/verify/${certificate.publicId}`}>공개 검증 화면</Link></div>
    {attestation && <p>발급 트랜잭션: <span className="monospace">{attestation.transactionHash}</span> ({attestation.status})</p>}
    {attestation?.status === "CONFIRMED" && certificate.status !== "REVOKED" && <div className="form-grid">
      <label>폐기 사유<input value={revocationReason} onChange={(event) => setRevocationReason(event.target.value)} maxLength={1000} /></label>
      <button className="button" onClick={revoke} disabled={working || attestation.revocationStatus === "PENDING"}>
        {attestation.revocationStatus === "PENDING" ? "폐기 확인 중..." : "온체인 폐기"}
      </button>
    </div>}
    {attestation?.revocationTransactionHash && <p>폐기 트랜잭션: <span className="monospace">{attestation.revocationTransactionHash}</span> ({attestation.revocationStatus})</p>}
    {error && <p className="error-message">{error}</p>}
    <details className="full-width"><summary>Canonical payload</summary><pre>{JSON.stringify(certificate.payload, null, 2)}</pre></details>
  </div>;
}
