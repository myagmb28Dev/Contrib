"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { useAuth } from "@/lib/auth-context";

export function ProfileDropdown() {
  const { user, logout } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const pathname = usePathname();

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setIsOpen(false);
      }
    }

    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
      document.addEventListener("keydown", handleKeyDown);
    }
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen]);

  // Close dropdown on route change
  useEffect(() => {
    setIsOpen(false);
  }, [pathname]);

  if (!user) return null;

  async function handleLogout() {
    setIsLoggingOut(true);
    try {
      await logout();
    } finally {
      setIsLoggingOut(false);
      setIsOpen(false);
    }
  }

  const menuItems = [
    { label: "대시보드", href: "/dashboard", exact: true },
    { label: "저장소 관리", href: "/dashboard/repositories" },
    { label: "기여 분석", href: "/dashboard/analyses" },
    { label: "인증서", href: "/dashboard/certificates" },
  ];

  return (
    <div className="profile-dropdown-container" ref={containerRef}>
      <button
        type="button"
        className={`profile-dropdown-trigger ${isOpen ? "active" : ""}`}
        onClick={() => setIsOpen((prev) => !prev)}
        aria-expanded={isOpen}
        aria-haspopup="true"
        aria-label="사용자 메뉴 열기"
      >
        <img
          src={`https://github.com/${user.githubUsername}.png`}
          alt={user.githubUsername}
          className="user-avatar"
          onError={(e) => {
            (e.target as HTMLElement).style.display = "none";
          }}
        />
        <span className="user-name">{user.githubUsername}</span>
        <span className="profile-chevron" aria-hidden="true">▼</span>
      </button>

      {isOpen && (
        <div className="profile-dropdown-menu" role="menu">
          <div className="profile-menu-header">
            <strong>{user.githubUsername}</strong>
            <small>{user.email ?? "GitHub 비공개 이메일"}</small>
          </div>

          {menuItems.map((item) => {
            const isActive = item.exact
              ? pathname === item.href
              : pathname.startsWith(item.href);

            return (
              <Link
                key={item.href}
                href={item.href}
                className={`profile-menu-item ${isActive ? "active" : ""}`}
                role="menuitem"
                onClick={() => setIsOpen(false)}
              >
                <span>{item.label}</span>
              </Link>
            );
          })}

          <div className="profile-menu-divider" />

          <button
            type="button"
            className="profile-menu-item profile-menu-logout"
            onClick={handleLogout}
            disabled={isLoggingOut}
            role="menuitem"
          >
            <span>{isLoggingOut ? "로그아웃 중..." : "로그아웃"}</span>
          </button>
        </div>
      )}
    </div>
  );
}
