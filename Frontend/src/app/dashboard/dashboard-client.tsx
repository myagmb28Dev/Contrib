"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import {
  ApiRequestError,
  type CurrentUser,
  getCurrentUser,
  logout,
} from "@/lib/api";

type DashboardState =
  | { status: "loading" }
  | { status: "authenticated"; user: CurrentUser }
  | { status: "unauthenticated" }
  | { status: "error"; message: string };

export function DashboardClient() {
  const router = useRouter();
  const [state, setState] = useState<DashboardState>({ status: "loading" });
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  useEffect(() => {
    const controller = new AbortController();

    getCurrentUser(controller.signal)
      .then((user) => setState({ status: "authenticated", user }))
      .catch((error: unknown) => {
        if (controller.signal.aborted) {
          return;
        }

        if (error instanceof ApiRequestError && error.status === 401) {
          setState({ status: "unauthenticated" });
          return;
        }

        setState({
          status: "error",
          message: error instanceof Error ? error.message : "알 수 없는 오류가 발생했어.",
        });
      });

    return () => controller.abort();
  }, []);

  async function handleLogout() {
    setIsLoggingOut(true);

    try {
      await logout();
      router.replace("/login");
      router.refresh();
    } catch (error) {
      setState({
        status: "error",
        message: error instanceof Error ? error.message : "로그아웃 중 오류가 발생했어.",
      });
      setIsLoggingOut(false);
    }
  }

  if (state.status === "loading") {
    return <p className="muted">로그인 정보를 확인하고 있어...</p>;
  }

  if (state.status === "unauthenticated") {
    return (
      <div className="stack">
        <p className="muted">로그인 세션이 없거나 만료됐어.</p>
        <Link className="button primary" href="/login">
          로그인 화면으로 이동
        </Link>
      </div>
    );
  }

  if (state.status === "error") {
    return <p className="error-message">{state.message}</p>;
  }

  return (
    <div className="stack">
      <dl className="identity-list">
        <div>
          <dt>GitHub 계정</dt>
          <dd>@{state.user.githubUsername}</dd>
        </div>
        <div>
          <dt>GitHub User ID</dt>
          <dd>{state.user.githubUserId}</dd>
        </div>
        <div>
          <dt>이메일</dt>
          <dd>{state.user.email ?? "GitHub에서 제공하지 않음"}</dd>
        </div>
        <div>
          <dt>내부 User ID</dt>
          <dd className="monospace">{state.user.userId}</dd>
        </div>
      </dl>
      <button className="button" type="button" onClick={handleLogout} disabled={isLoggingOut}>
        {isLoggingOut ? "로그아웃 중..." : "로그아웃"}
      </button>
    </div>
  );
}
