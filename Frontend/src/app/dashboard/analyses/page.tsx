import { AnalysesClient } from "@/components/analyses-client";
import { AppHeader } from "@/components/app-header";

export default function AnalysesPage() {
  return (
    <>
      <AppHeader />
      <main className="shell">
        <div className="page-header">
          <div>
            <p className="eyebrow">CONTRIBUTION ATTESTATION</p>
            <h1>기여 분석 이력</h1>
            <p className="muted">
              저장소별 활동 스냅샷을 기반으로 계산된 기여 점수와 Gemini AI 분석 요약 목록입니다.
            </p>
          </div>
        </div>
        <AnalysesClient />
      </main>
    </>
  );
}
