"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { ApiRequestError, getCurrentUser, logout, type CurrentUser } from "@/lib/api";

export function AppHeader() {
  const pathname = usePathname();
  const router = useRouter();
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    getCurrentUser(controller.signal)
      .then(setUser)
      .catch((error: unknown) => {
        if (error instanceof ApiRequestError && error.status === 401) {
          setUser(null);
          if (pathname !== "/" && !pathname.startsWith("/verify")) {
            router.replace("/");
          }
        }
      });
    return () => controller.abort();
  }, [pathname, router]);

  async function handleLogout() {
    setIsLoggingOut(true);
    try {
      await logout();
      router.replace("/");
      router.refresh();
    } catch {
      setIsLoggingOut(false);
    }
  }

  const navItems = [
    { label: "계정 개요", href: "/dashboard", exact: true },
    { label: "저장소 관리", href: "/dashboard/repositories" },
    { label: "기여 분석", href: "/dashboard/analyses" },
    { label: "인증서", href: "/dashboard/certificates" },
  ];

  return (
    <header className="app-header">
      <div className="app-header-inner">
        <div className="header-left">
          <Link href="/" className="brand" aria-label="Contrib 홈">
            <span className="brand-text">Contrib</span>
          </Link>
          <nav className="header-nav" aria-label="메인 내비게이션">
            {navItems.map((item) => {
              const isActive = item.exact
                ? pathname === item.href
                : pathname.startsWith(item.href);
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`nav-tab ${isActive ? "active" : ""}`}
                >
                  {item.label}
                </Link>
              );
            })}
          </nav>
        </div>

        <div className="header-right">
          <Link href="/#verify" className="header-link">
            공개 검증
          </Link>
          {user ? (
            <div className="user-menu">
              <div className="user-badge" title={`User ID: ${user.userId}`}>
                <img
                  src={`https://github.com/${user.githubUsername}.png`}
                  alt={`@${user.githubUsername}`}
                  className="user-avatar"
                  onError={(e) => {
                    (e.target as HTMLElement).style.display = "none";
                  }}
                />
                <span className="user-name">@{user.githubUsername}</span>
              </div>
              <button
                type="button"
                className="header-logout-button"
                onClick={handleLogout}
                disabled={isLoggingOut}
                title="로그아웃"
              >
                {isLoggingOut ? "..." : "로그아웃"}
              </button>
            </div>
          ) : (
            <Link href="/" className="button primary sm">
              로그인
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}
