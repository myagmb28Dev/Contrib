"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ProfileDropdown } from "@/components/profile-dropdown";
import { PublicVerificationForm } from "@/components/public-verification-form";
import { apiBaseUrl } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";

const githubLoginUrl = `${apiBaseUrl}/api/auth/github`;

function TypewriterText({ text, speed = 35 }: { text: string; speed?: number }) {
  const [displayed, setDisplayed] = useState("");
  const [index, setIndex] = useState(0);
  const [replayKey, setReplayKey] = useState(0);

  useEffect(() => {
    setDisplayed("");
    setIndex(0);
  }, [text, replayKey]);

  useEffect(() => {
    if (index < text.length) {
      const timer = setTimeout(() => {
        setDisplayed((prev) => prev + text.charAt(index));
        setIndex((prev) => prev + 1);
      }, speed);
      return () => clearTimeout(timer);
    }
  }, [index, text, speed, replayKey]);

  function handleReplay() {
    setReplayKey((prev) => prev + 1);
  }

  return (
    <span
      className="typewriter-interactive"
      onClick={handleReplay}
      title="클릭하면 타이핑 애니메이션이 처음부터 다시 재생됩니다"
    >
      {displayed}
      <span className="typing-cursor" aria-hidden="true" />
    </span>
  );
}

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
            <p className="hero-description">
              <TypewriterText text={"공개 저장소 활동을 스냅샷으로 고정하고, 일관된 기준으로 분석해\n누구나 확인할 수 있는 Contribution Certificate를 생성합니다."} />
            </p>
            <p className="privacy-note">
              <span aria-hidden="true">●</span> 공개 저장소만 안전하게 분석하며, 객관적인 기여 점수와 AI 요약 리포트를 함께 제공합니다.
            </p>
          </div>

          <div className="certificate-stage" aria-label="Contribution Certificate 미리보기">
            <div className="certificate-glow" />
            <article className="certificate-preview">
              <div className="cert-card-header">
                <div className="cert-title-group">
                  <span className="cert-sub-label">CONTRIBUTION CERTIFICATE</span>
                  <strong className="cert-repo-name">myagmb28Dev / Contrib</strong>
                </div>
                <div className="cert-header-badges">
                  <span className="score-pill" style={{ fontWeight: 700, fontSize: "0.82rem", color: "var(--primary)", background: "var(--primary-light)", padding: "3px 8px", borderRadius: "9999px" }}>
                    88점
                  </span>
                  <span className="verified-badge valid">VALID</span>
                  <span className="network-tag">Base Sepolia</span>
                </div>
              </div>

              <div className="cert-public-id-bar">
                <span className="cert-id-tag">Public ID</span>
                <code className="cert-id-val">3a8f9b2c-e12d-4567-89ab-cdef01234567</code>
                <div className="cert-id-btn-group">
                  <span className="cert-id-copy-action">ID 복사</span>
                  <span className="cert-id-copy-action highlight">링크 복사</span>
                </div>
              </div>

              <div className="cert-meta-grid">
                <div>
                  <span className="meta-label">Subject Wallet</span>
                  <span className="meta-val monospace">0x71C8...b29F</span>
                </div>
                <div>
                  <span className="meta-label">발급 일시</span>
                  <span className="meta-val">2026. 8. 28.</span>
                </div>
              </div>

              <div className="area-tags" aria-label="기술 영역">
                <span>TypeScript</span>
                <span>Next.js</span>
                <span>Spring Boot</span>
                <span>Smart Contract</span>
              </div>

              <footer className="certificate-footer-row">
                <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                  <span className="muted" style={{ fontSize: "0.75rem" }}>Snapshot hash:</span>
                  <code style={{ fontSize: "0.78rem" }}>0x83f1...c42a</code>
                </div>
                <span style={{ fontSize: "0.76rem", color: "#059669", fontWeight: 700 }}>
                  ● 온체인 검증 완료
                </span>
              </footer>
            </article>
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
          <a className="brand footer-brand" href="/">
            <span className="brand-text">Contrib</span>
          </a>
          <p>GitHub 활동 기반의 검증 가능한 Contribution Certificate</p>
        </footer>
      </div>
    </main>
  );
}
