import Link from "next/link";
import { redirect } from "next/navigation";

import { apiBaseUrl } from "@/lib/api";

type LoginPageProps = {
  searchParams: Promise<{ error?: string }>;
};

export default async function LoginPage({ searchParams }: LoginPageProps) {
  const { error } = await searchParams;
  const githubLoginUrl = `${apiBaseUrl}/api/auth/github`;

  if (error !== "oauth") {
    redirect(githubLoginUrl);
  }

  return (
    <main className="shell narrow-shell">
      <p className="eyebrow">Contribution Attestation</p>
      <div className="card stack auth-error-card">
        <div>
          <h1>GitHub 로그인을 완료하지 못했습니다.</h1>
          <p className="muted">
            GitHub에서 권한 승인이 취소되었거나 일시적인 문제가 발생했습니다. 다시 시도해도 계정
            데이터는 변경되지 않습니다.
          </p>
        </div>
        <div className="actions">
          <a className="button primary" href={githubLoginUrl}>
            GitHub 로그인 다시 시도
          </a>
          <Link className="button" href="/">
            홈으로 돌아가기
          </Link>
        </div>
      </div>
    </main>
  );
}
