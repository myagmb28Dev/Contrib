import { DashboardClient } from "./dashboard-client";
import { AppNav } from "@/components/app-nav";

export default function DashboardPage() {
  return (
    <main className="shell narrow-shell">
      <p className="eyebrow">Contribution Attestation</p>
      <AppNav />
      <div className="card stack">
        <div>
          <h1>내 계정</h1>
          <p className="muted">GitHub OAuth로 연결된 현재 사용자 정보입니다.</p>
        </div>
        <DashboardClient />
      </div>
    </main>
  );
}
