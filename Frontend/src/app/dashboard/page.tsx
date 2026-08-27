import { AppHeader } from "@/components/app-header";
import { DashboardClient } from "./dashboard-client";

export default function DashboardPage() {
  return (
    <>
      <AppHeader />
      <main className="shell">
        <div className="page-header">
          <div>
            <p className="eyebrow">CONTRIBUTION ATTESTATION</p>
            <h1>내 계정 대시보드</h1>
            <p className="muted">GitHub 기여도 분석과 온체인 인증서 발급 현황을 한눈에 관리합니다.</p>
          </div>
        </div>
        <DashboardClient />
      </main>
    </>
  );
}
