import Link from "next/link";

export function AppNav() {
  return (
    <nav className="nav" aria-label="대시보드 메뉴">
      <Link href="/dashboard">계정</Link>
      <Link href="/dashboard/repositories">저장소</Link>
      <Link href="/dashboard/analyses">분석</Link>
      <Link href="/dashboard/certificates">인증서</Link>
    </nav>
  );
}
