"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import { ProfileDropdown } from "./profile-dropdown";
import { useAuth } from "@/lib/auth-context";

export function AppHeader() {
  const pathname = usePathname();
  const { user } = useAuth();

  const navItems = [
    { label: "대시보드", href: "/dashboard", exact: true },
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
            <ProfileDropdown />
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
