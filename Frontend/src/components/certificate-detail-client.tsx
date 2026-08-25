"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { createWalletClient, custom, type EIP1193Provider } from "viem";

import { getAttestationIntent, getCertificate, submitAttestation, type Attestation, type Certificate } from "@/lib/api";

const attestationAbi = [{
  type: "function", name: "issue", stateMutability: "nonpayable",
  inputs: [{ name: "certificateId", type: "bytes32" }, { name: "certificateHash", type: "bytes32" }, { name: "subject", type: "address" }],
  outputs: [],
}] as const;

declare global { interface Window { ethereum?: EIP1193Provider } }

export function CertificateDetailClient({ certificateId }: { certificateId: string }) {
  const [certificate, setCertificate] = useState<Certificate | null>(null);
  const [attestation, setAttestation] = useState<Attestation | null>(null);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => { getCertificate(certificateId).then(setCertificate).catch((reason: Error) => setError(reason.message)); }, [certificateId]);

  async function publish() {
    if (!window.ethereum) { setError("브라우저 지갑을 찾을 수 없습니다."); return; }
    setWorking(true); setError("");
    try {
      const intent = await getAttestationIntent(certificateId);
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
          blockExplorerUrls: ["https://sepolia-explorer.base.org"],
        }] });
      }
      const hash = await wallet.writeContract({ account, chain: null, address: intent.contractAddress, abi: attestationAbi,
        functionName: "issue", args: intent.arguments });
      setAttestation(await submitAttestation(certificateId, hash, account));
    } catch (reason) { setError(reason instanceof Error ? reason.message : "온체인 발급에 실패했습니다."); }
    finally { setWorking(false); }
  }

  if (!certificate) return <p className={error ? "error-message" : "muted"}>{error || "인증서를 불러오는 중입니다..."}</p>;
  return <div className="stack full-width">
    <dl className="identity-list">
      <div><dt>상태</dt><dd>{certificate.status}</dd></div>
      <div><dt>Hash</dt><dd className="monospace">{certificate.hash}</dd></div>
      <div><dt>Subject Wallet</dt><dd className="monospace">{certificate.subjectWalletAddress ?? "미설정"}</dd></div>
      <div><dt>Public ID</dt><dd className="monospace">{certificate.publicId}</dd></div>
    </dl>
    <div className="actions"><button className="button primary" onClick={publish} disabled={working || !certificate.subjectWalletAddress}>{working ? "지갑 확인 중..." : "온체인 발급"}</button>
      <Link className="button" href={`/verify/${certificate.publicId}`}>공개 검증 화면</Link></div>
    {attestation && <p>트랜잭션: <span className="monospace">{attestation.transactionHash}</span> ({attestation.status})</p>}
    {error && <p className="error-message">{error}</p>}
    <details className="full-width"><summary>Canonical payload</summary><pre>{JSON.stringify(certificate.payload, null, 2)}</pre></details>
  </div>;
}
