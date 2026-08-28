"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import {
  ApiRequestError,
  apiBaseUrl,
  getAnalyses,
  getCertificates,
  getCurrentUser,
  getRepositories,
  logout,
  type CurrentUser,
} from "@/lib/api";

type DashboardState =
  | { status: "loading" }
  | { status: "authenticated"; user: CurrentUser }
  | { status: "error"; message: string };

export function DashboardClient() {
  const router = useRouter();
  const [state, setState] = useState<DashboardState>({ status: "loading" });
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  const [stats, setStats] = useState<{
    repoCount: number | null;
    analysisCount: number | null;
    certCount: number | null;
  }>({
    repoCount: null,
    analysisCount: null,
    certCount: null,
  });

  useEffect(() => {
    const controller = new AbortController();

    getCurrentUser(controller.signal)
      .then((user) => {
        setState({ status: "authenticated", user });
        Promise.allSettled([
          getRepositories(),
          getAnalyses(),
          getCertificates(),
        ]).then(([repos, analyses, certs]) => {
          setStats({
            repoCount: repos.status === "fulfilled" ? repos.value.length : 0,
            analysisCount: analyses.status === "fulfilled" ? analyses.value.length : 0,
            certCount: certs.status === "fulfilled" ? certs.value.length : 0,
          });
        });
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) return;

        if (error instanceof ApiRequestError && error.status === 401) {
          router.replace("/");
          return;
        }

        setState({
          status: "error",
          message: error instanceof Error ? error.message : "알 수 없는 오류가 발생했습니다.",
        });
      });

    return () => controller.abort();
  }, [router]);

  async function handleLogout() {
    setIsLoggingOut(true);
    try {
      await logout();
      router.replace("/");
      router.refresh();
    } catch (error) {
      setState({
        status: "error",
        message: error instanceof Error ? error.message : "로그아웃 중 오류가 발생했습니다.",
      });
      setIsLoggingOut(false);
    }
  }

  function copyToClipboard(text: string, key: string) {
    navigator.clipboard.writeText(text).then(() => {
      setCopiedKey(key);
      setTimeout(() => setCopiedKey(null), 2000);
    });
  }

  if (state.status === "loading") {
    return (
      <div className="card loading-card">
        <div className="skeleton-line lg" />
        <div className="skeleton-line md" />
        <p className="muted">로그인 정보를 확인하는 중입니다...</p>
      </div>
    );
  }

  if (state.status === "error") {
    return (
      <div className="card stack">
        <p className="error-message">{state.message}</p>
        <a className="button" href={`${apiBaseUrl}/api/auth/github`}>
          로그인 다시 시도
        </a>
      </div>
    );
  }

  const { user } = state;

  return (
    <div className="dashboard-layout">
      {/* Profile Overview Card */}
      <section className="card profile-hero-card">
        <div className="profile-hero-content">
          <img
            src={`https://github.com/${user.githubUsername}.png`}
            alt={`@${user.githubUsername}`}
            className="profile-hero-avatar"
            onError={(e) => {
              (e.target as HTMLElement).style.display = "none";
            }}
          />
          <div className="profile-hero-info">
            <div className="profile-badge-row">
              <span className="verified-badge">GitHub 연결됨</span>
              <span className="ai-badge">AI: Google Gemini 2.0 Flash</span>
            </div>
            <h2>@{user.githubUsername}</h2>
            <p className="muted">{user.email ?? "GitHub 비공개 이메일"}</p>
          </div>
        </div>

        <div className="profile-id-pills">
          <div className="id-pill">
            <span className="pill-label">GitHub User ID</span>
            <span className="pill-value monospace">{user.githubUserId}</span>
          </div>
          <div className="id-pill">
            <span className="pill-label">내부 User ID</span>
            <span className="pill-value monospace">{user.userId}</span>
            <button
              type="button"
              className="copy-btn"
              onClick={() => copyToClipboard(user.userId, "userId")}
              title="ID 복사"
            >
              {copiedKey === "userId" ? "복사됨" : "복사"}
            </button>
          </div>
        </div>
      </section>

      {/* Quick Stats Grid */}
      <section className="stats-grid" aria-label="기여 활동 통계">
        <div className="stat-card">
          <div className="stat-card-header">
            <span className="stat-label">동기화된 저장소</span>
          </div>
          <strong className="stat-value">
            {stats.repoCount === null ? "..." : `${stats.repoCount}개`}
          </strong>
          <Link href="/dashboard/repositories" className="stat-link">
            저장소 관리 &rarr;
          </Link>
        </div>

        <div className="stat-card">
          <div className="stat-card-header">
            <span className="stat-label">완료된 기여 분석</span>
          </div>
          <strong className="stat-value">
            {stats.analysisCount === null ? "..." : `${stats.analysisCount}건`}
          </strong>
          <Link href="/dashboard/analyses" className="stat-link">
            분석 목록 &rarr;
          </Link>
        </div>

        <div className="stat-card">
          <div className="stat-card-header">
            <span className="stat-label">발급된 인증서</span>
          </div>
          <strong className="stat-value">
            {stats.certCount === null ? "..." : `${stats.certCount}건`}
          </strong>
          <Link href="/dashboard/certificates" className="stat-link">
            인증서 목록 &rarr;
          </Link>
        </div>

        <div className="stat-card">
          <div className="stat-card-header">
            <span className="stat-label">온체인 네트워크</span>
          </div>
          <strong className="stat-value">Base Sepolia</strong>
          <span className="stat-subtext">Chain ID: 84532</span>
        </div>
      </section>

      {/* Quick Actions Grid */}
      <section className="quick-actions-section">
        <h3 className="section-title">빠른 작업</h3>
        <div className="action-cards-grid">
          <Link href="/dashboard/repositories" className="action-card">
            <div>
              <strong>저장소 동기화 및 분석</strong>
              <p>GitHub 공개 저장소를 가져와 새 기여도 분석을 시작합니다.</p>
            </div>
          </Link>

          <Link href="/dashboard/analyses" className="action-card">
            <div>
              <strong>기여 분석 결과 검토</strong>
              <p>Gemini 2.0 Flash가 요약한 기여 지표와 점수를 확인합니다.</p>
            </div>
          </Link>

          <Link href="/dashboard/certificates" className="action-card">
            <div>
              <strong>온체인 인증서 발급 및 관리</strong>
              <p>Base Sepolia 스마트 컨트랙트에 기여 증명을 기록합니다.</p>
            </div>
          </Link>

          <Link href="/#verify" className="action-card">
            <div>
              <strong>공개 인증서 검증</strong>
              <p>Public ID 또는 해시를 통해 진위 여부를 직접 검증합니다.</p>
            </div>
          </Link>
        </div>
      </section>

      {/* Account Settings & Logout */}
      <div className="dashboard-footer-card card">
        <div>
          <h4>계정 세션 관리</h4>
          <p className="muted">GitHub OAuth 인증 세션을 종료하고 안전하게 로그아웃합니다.</p>
        </div>
        <button className="button danger-outline" type="button" onClick={handleLogout} disabled={isLoggingOut}>
          {isLoggingOut ? "로그아웃 중..." : "세션 로그아웃"}
        </button>
      </div>
    </div>
  );
}
