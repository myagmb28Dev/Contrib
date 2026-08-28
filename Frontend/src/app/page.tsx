"use client";

import Link from "next/link";
import { useState } from "react";
import { ProfileDropdown } from "@/components/profile-dropdown";
import { PublicVerificationForm } from "@/components/public-verification-form";
import { TypewriterText } from "@/components/typewriter";
import { apiBaseUrl } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";

const githubLoginUrl = `${apiBaseUrl}/api/auth/github`;

const DUMMY_PUBLIC_ID = "c8d4e2a1-9b7f-4567-a890-123456789abc";

function GitHubMark() {
  return (
    <svg aria-hidden="true" viewBox="0 0 24 24" width="20" height="20">
      <path
        fill="currentColor"
        d="M12 .7a11.5 11.5 0 0 0-3.64 22.41c.58.11.79-.25.79-.56v-2.23c-3.23.7-3.91-1.37-3.91-1.37-.53-1.34-1.29-1.7-1.29-1.7-1.05-.72.08-.71.08-.71 1.17.08 1.78 1.2 1.78 1.2 1.04 1.77 2.72 1.26 3.39.96.1-.75.4-1.26.74-1.55-2.58-.29-5.29-1.29-5.29-5.68 0-1.26.45-2.29 1.19-3.09-.12-.29-.52-1.47.11-3.05 0 0 .97-.31 3.16 1.18a10.94 10.94 0 0 1 5.76 0c2.19-1.49 3.16-1.18 3.16-1.18.63 1.58.23 2.76.11 3.05.74.8 1.19 1.83 1.19 3.09 0 4.41-2.72 5.38-5.31 5.67.42.36.79 1.07.79 2.16v3.2c0 .31.21.67.8.56A11.5 11.5 0 0 0 12 .7Z"
      />
    </svg>
  );
}

export default function HomePage() {
  const { user, loading } = useAuth();
  const [copiedType, setCopiedType] = useState<"id" | "link" | null>(null);
  const [isFlipped, setIsFlipped] = useState(false);
  const [tilt, setTilt] = useState({ x: 0, y: 0 });

  function handleMouseMove(e: React.MouseEvent<HTMLDivElement>) {
    const rect = e.currentTarget.getBoundingClientRect();
    const x = (e.clientX - rect.left) / rect.width - 0.5;
    const y = (e.clientY - rect.top) / rect.height - 0.5;
    setTilt({ x: -(y * 12), y: x * 12 });
  }

  function handleMouseLeave() {
    setTilt({ x: 0, y: 0 });
  }

  function copyDummyId(e: React.MouseEvent) {
    e.stopPropagation();
    navigator.clipboard.writeText(DUMMY_PUBLIC_ID);
    setCopiedType("id");
    setTimeout(() => setCopiedType(null), 2000);
  }

  function copyDummyLink(e: React.MouseEvent) {
    e.stopPropagation();
    const url = typeof window !== "undefined"
      ? `${window.location.origin}/verify/${DUMMY_PUBLIC_ID}`
      : `https://contrib.dev/verify/${DUMMY_PUBLIC_ID}`;
    navigator.clipboard.writeText(url);
    setCopiedType("link");
    setTimeout(() => setCopiedType(null), 2000);
  }

  return (
    <main className="landing-page">
      <div className="landing-shell">
        <header className="landing-header">
          <Link className="brand" href="/" aria-label="Contrib 홈">
            <span className="brand-text">Contrib</span>
          </Link>

          <div className="landing-header-right">
            {!loading && user ? (
              <ProfileDropdown />
            ) : (
              <a className="header-login" href={githubLoginUrl}>
                <GitHubMark />
                GitHub 로그인
              </a>
            )}
          </div>
        </header>

        <section className="landing-hero">
          <div className="hero-copy">
            <p className="eyebrow">GitHub Contribution Attestation</p>
            <h1>
              GitHub 기여를,
              <span>검증 가능한 경력 증명으로.</span>
            </h1>
            <p className="hero-description" style={{ minHeight: "56px" }}>
              <TypewriterText text={"공개 저장소 활동을 스냅샷으로 고정하고, 일관된 기준으로 분석해\n누구나 확인할 수 있는 Contribution Certificate를 생성합니다."} />
            </p>
            <p className="privacy-note">
              <span aria-hidden="true">●</span> 공개 저장소만 안전하게 분석하며, 객관적인 기여 점수와 AI 요약 리포트를 함께 제공합니다.
            </p>
          </div>

          <div className="certificate-stage" aria-label="Contribution Certificate 3D 인터랙티브 미리보기">
            <div className="certificate-glow" />
            <div
              className="certificate-3d-scene"
              onMouseMove={handleMouseMove}
              onMouseLeave={handleMouseLeave}
            >
              <div
                className={`certificate-3d-card ${isFlipped ? "flipped" : ""}`}
                style={{
                  transform: isFlipped
                    ? `rotateY(180deg) rotateX(${tilt.x}deg) rotateY(${-tilt.y}deg)`
                    : `rotateX(${tilt.x}deg) rotateY(${tilt.y}deg)`,
                }}
                onClick={() => setIsFlipped((prev) => !prev)}
              >
                {/* FRONT SIDE */}
                <article className="certificate-preview certificate-front">
                  <div className="cert-card-header">
                    <div className="cert-title-group">
                      <span className="cert-sub-label">CONTRIBUTION CERTIFICATE</span>
                      <strong className="cert-repo-name">hyperion-core / quantum-mesh</strong>
                    </div>
                    <div className="cert-header-badges">
                      <span className="score-pill" style={{ fontWeight: 700, fontSize: "0.82rem", color: "var(--primary)", background: "var(--primary-light)", padding: "3px 8px", borderRadius: "9999px" }}>
                        94점
                      </span>
                      <span className="verified-badge valid">VALID</span>
                      <span className="network-tag">Base Sepolia</span>
                    </div>
                  </div>

                  <div className="cert-public-id-bar">
                    <span className="cert-id-tag">Public ID</span>
                    <code className="cert-id-val">{DUMMY_PUBLIC_ID}</code>
                    <div className="cert-id-btn-group">
                      <button
                        type="button"
                        className="cert-id-copy-action"
                        onClick={copyDummyId}
                        style={{ cursor: "pointer", border: "none", background: "none", fontFamily: "inherit" }}
                      >
                        {copiedType === "id" ? "ID 복사됨!" : "ID 복사"}
                      </button>
                      <button
                        type="button"
                        className="cert-id-copy-action highlight"
                        onClick={copyDummyLink}
                        style={{ cursor: "pointer", border: "none", background: "none", fontFamily: "inherit" }}
                      >
                        {copiedType === "link" ? "링크 복사됨!" : "링크 복사"}
                      </button>
                    </div>
                  </div>

                  <div className="cert-meta-grid">
                    <div>
                      <span className="meta-label">Subject Wallet</span>
                      <span className="meta-val monospace">0x8920...43e7</span>
                    </div>
                    <div>
                      <span className="meta-label">발급 일시</span>
                      <span className="meta-val">2026. 8. 28.</span>
                    </div>
                  </div>

                  <div className="area-tags" aria-label="기술 영역">
                    <span>Rust</span>
                    <span>Distributed Systems</span>
                    <span>Wasm</span>
                    <span>Zero Knowledge</span>
                  </div>

                  <footer className="certificate-footer-row">
                    <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                      <span className="muted" style={{ fontSize: "0.75rem" }}>Snapshot hash:</span>
                      <code style={{ fontSize: "0.78rem" }}>0x7a3f...b91d</code>
                    </div>
                    <span style={{ fontSize: "0.76rem", color: "#059669", fontWeight: 700 }}>
                      ● 온체인 검증 완료
                    </span>
                  </footer>
                </article>

                {/* BACK SIDE (3D FLIPPED) */}
                <article className="certificate-preview certificate-back">
                  <div className="cert-card-header">
                    <div className="cert-title-group">
                      <span className="cert-sub-label" style={{ color: "#818cf8" }}>EAS ON-CHAIN PROOF</span>
                      <strong className="cert-repo-name" style={{ color: "#ffffff" }}>
                        Attestation Smart Contract
                      </strong>
                    </div>
                    <div className="cert-header-badges">
                      <span className="network-tag" style={{ background: "rgba(129, 140, 248, 0.2)", color: "#c7d2fe", border: "1px solid rgba(129, 140, 248, 0.4)" }}>
                        Base Sepolia (84532)
                      </span>
                    </div>
                  </div>

                  <div className="cert-back-metric-grid">
                    <div className="cert-back-metric-item">
                      <span className="cert-back-metric-label">Commits</span>
                      <span className="cert-back-metric-val">142건 (+14.2k / -3.8k)</span>
                    </div>
                    <div className="cert-back-metric-item">
                      <span className="cert-back-metric-label">PRs Merged</span>
                      <span className="cert-back-metric-val">38건 (Approved)</span>
                    </div>
                    <div className="cert-back-metric-item">
                      <span className="cert-back-metric-label">Code Reviews</span>
                      <span className="cert-back-metric-val">65건 제출</span>
                    </div>
                    <div className="cert-back-metric-item">
                      <span className="cert-back-metric-label">Active Period</span>
                      <span className="cert-back-metric-val">180 Days Active</span>
                    </div>
                  </div>

                  <div className="cert-meta-grid" style={{ background: "rgba(0,0,0,0.25)", padding: "8px 12px", borderRadius: "8px" }}>
                    <div>
                      <span className="meta-label" style={{ color: "#94a3b8" }}>Schema UID</span>
                      <span className="meta-val monospace" style={{ color: "#cbd5e1" }}>0xd342...9a12</span>
                    </div>
                    <div>
                      <span className="meta-label" style={{ color: "#94a3b8" }}>Attester Node</span>
                      <span className="meta-val" style={{ color: "#cbd5e1" }}>Contrib Oracle #04</span>
                    </div>
                  </div>

                  <div className="cert-back-signature">
                    <span>ECDSA Sig: <code style={{ color: "#38bdf8" }}>0x3c91...8f2b</code></span>
                    <span style={{ color: "#34d399", fontWeight: 700 }}>● Verified</span>
                  </div>

                  <footer className="certificate-footer-row" style={{ borderColor: "rgba(255, 255, 255, 0.1)" }}>
                    <span style={{ fontSize: "0.75rem", color: "#94a3b8" }}>
                      Merkle Root: <code style={{ color: "#e2e8f0" }}>0x7a3f8c...b91d</code>
                    </span>
                    <span style={{ fontSize: "0.74rem", color: "#818cf8", fontWeight: 700 }}>
                      Non-Fungible Attestation
                    </span>
                  </footer>
                </article>
              </div>
            </div>
          </div>
        </section>

        <section className="benefit-grid" aria-label="서비스 특징">
          <article>
            <span className="benefit-icon">01</span>
            <div>
              <h2>관찰 가능한 기록</h2>
              <p>커밋, PR, 리뷰 활동을 기간별 Snapshot으로 기록합니다.</p>
            </div>
          </article>
          <article>
            <span className="benefit-icon">02</span>
            <div>
              <h2>설명 가능한 점수</h2>
              <p>공개된 규칙과 버전으로 일관된 결과를 산출합니다.</p>
            </div>
          </article>
          <article>
            <span className="benefit-icon">03</span>
            <div>
              <h2>누구나 공개 검증</h2>
              <p>Hash와 온체인 기록으로 인증서 상태를 직접 확인할 수 있습니다.</p>
            </div>
          </article>
        </section>

        <section className="process-section">
          <div className="section-heading">
            <p className="eyebrow">How it works</p>
            <h2>기여가 증명이 되는 네 단계</h2>
          </div>
          <ol className="process-list">
            <li>
              <span>1</span>
              <strong>GitHub 연결</strong>
              <p>계정을 연결하고 공개 저장소를 선택합니다.</p>
            </li>
            <li>
              <span>2</span>
              <strong>활동 분석</strong>
              <p>기간별 활동과 기여 지표를 수집합니다.</p>
            </li>
            <li>
              <span>3</span>
              <strong>인증서 생성</strong>
              <p>점수와 요약이 포함된 Certificate를 생성합니다.</p>
            </li>
            <li>
              <span>4</span>
              <strong>공개 검증</strong>
              <p>공개 ID와 온체인 Hash로 상태를 확인합니다.</p>
            </li>
          </ol>
        </section>

        <PublicVerificationForm />

        <footer className="landing-footer">
          <Link className="brand footer-brand" href="/">
            <span className="brand-text">Contrib</span>
          </Link>
          <p>GitHub 활동 기반의 검증 가능한 Contribution Certificate</p>
        </footer>
      </div>
    </main>
  );
}
