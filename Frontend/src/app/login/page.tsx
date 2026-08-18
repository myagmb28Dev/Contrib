import { apiBaseUrl } from "@/lib/api";

export default function LoginPage() {
  return (
    <main className="shell narrow-shell">
      <p className="eyebrow">Contribution Attestation</p>
      <div className="card stack">
        <div>
          <h1>GitHub 로그인</h1>
          <p className="muted">
            GitHub 계정을 연결하면 Repository 활동을 가져와 기여 분석을 시작할 수 있어.
          </p>
        </div>
        <a className="button primary" href={`${apiBaseUrl}/api/auth/github`}>
          GitHub로 계속하기
        </a>
      </div>
    </main>
  );
}
