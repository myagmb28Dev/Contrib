import Link from "next/link";

export default function HomePage() {
  return (
    <main className="shell">
      <p className="eyebrow">GitHub Contribution Attestation</p>
      <h1>관찰된 GitHub 활동을 검증 가능한 Certificate로</h1>
      <p className="muted">
        Repository 활동을 Snapshot으로 고정하고, 규칙 기반 Score와 공개 검증 가능한 Hash를 제공해.
      </p>
      <nav className="nav" aria-label="주요 메뉴">
        <Link href="/login">GitHub로 시작하기</Link>
      </nav>
    </main>
  );
}
