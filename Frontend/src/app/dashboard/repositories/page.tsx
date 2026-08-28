import { AppHeader } from "@/components/app-header";
import { RepositoriesClient } from "@/components/repositories-client";

export default function RepositoriesPage() {
  return (
    <>
      <AppHeader />
      <main className="shell">
        <div className="page-header">
          <div>
            <p className="eyebrow">CONTRIBUTION ATTESTATION</p>
            <h1>저장소 관리</h1>
            <p className="muted">GitHub 공개 저장소를 동기화하고 활동 분석을 수행할 저장소를 선택하세요.</p>
          </div>
        </div>
        <RepositoriesClient />
      </main>
    </>
  );
}
