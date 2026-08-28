"use client";

import Link from "next/link";
import { ProfileDropdown } from "@/components/profile-dropdown";
import { PublicVerificationForm } from "@/components/public-verification-form";
import { apiBaseUrl } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";

const githubLoginUrl = `${apiBaseUrl}/api/auth/github`;

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
              공개 저장소 활동을 스냅샷으로 고정하고, 일관된 기준으로 분석해 누구나 확인할 수 있는
              Contribution Certificate를 생성합니다.
            </p>
            <p className="privacy-note">
              <span aria-hidden="true">●</span> 공개 저장소 활동만 수집하며, 점수 계산과 Gemini AI 요약은 분리하여 제공합니다.
            </p>
          </div>

          <div className="certificate-stage" aria-label="Contribution Certificate 미리보기">
            <div className="certificate-glow" />
            <article className="certificate-preview">
              <header className="certificate-heading">
                <div>
                  <span className="preview-label">CONTRIBUTION CERTIFICATE</span>
                  <strong>Verified contribution</strong>
                </div>
                <span className="verified-badge">VERIFIED</span>
              </header>
              <div className="certificate-repository">
                <span>Repository</span>
                <strong>yourname / meaningful-project</strong>
              </div>
              <div className="certificate-score-row">
                <div className="preview-score">
                  <span>Contribution score</span>
                  <strong>82</strong>
                  <small>/ 100</small>
                </div>
                <div className="mini-chart" aria-hidden="true">
                  <i style={{ height: "38%" }} />
                  <i style={{ height: "64%" }} />
                  <i style={{ height: "48%" }} />
                  <i style={{ height: "84%" }} />
                  <i style={{ height: "72%" }} />
                  <i style={{ height: "100%" }} />
                </div>
              </div>
              <div className="area-tags" aria-label="기술 영역 예시">
                <span>Java</span>
                <span>Backend</span>
                <span>Code Review</span>
              </div>
              <footer className="certificate-footer">
                <span>Snapshot hash</span>
                <code>0x83f1...c42a</code>
                <span className="chain-dot">Base Sepolia</span>
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
