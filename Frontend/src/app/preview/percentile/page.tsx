import { notFound } from "next/navigation";
import { ActivityComparisonCard } from "@/components/activity-comparison";
import type { ActivityComparison } from "@/lib/api";

export const dynamic = "force-dynamic";

const sample: ActivityComparison = {
  status: "AVAILABLE",
  modelVersion: "activity-percentile-v1",
  percentile: 80,
  metricPercentiles: { commits: 75, pullRequestsOpened: 85, reviews: 70, activeDays: 90 },
  metricWeights: { commits: 0.25, pullRequestsOpened: 0.25, reviews: 0.25, activeDays: 0.25 },
  datasetId: null,
  cohortId: null,
  referenceCollectedAt: null,
  periodStart: "2026-06-01T00:00:00Z",
  periodEnd: "2026-09-01T00:00:00Z",
  language: "C#",
  sizeBand: "10-49",
  sampleCount: 32,
  contributorCount: 30,
  repositories: ["샘플 저장소 A", "샘플 저장소 B", "샘플 저장소 C"],
  scope: "화면 구성을 설명하기 위해 만든 가상 데이터입니다.",
  calculationRules: "항목별 백분위와 비중, 비교 집단 정보를 표시하는 예시입니다.",
  reason: "미리보기용 샘플입니다. 실제 GitHub 기여 분석 결과가 아닙니다.",
  singleActivitySensitivity: 30,
};

export default function PercentilePreviewPage() {
  if (process.env.ENABLE_PERCENTILE_PREVIEW !== "true") notFound();
  return <main className="shell narrow-shell stack" style={{ paddingTop: 32, paddingBottom: 32 }}>
    <header>
      <p className="eyebrow">SAMPLE PREVIEW</p>
      <h1>백분위 미리보기</h1>
      <p className="muted">아래 숫자는 화면 확인용 샘플입니다. 실제 분석이나 인증서 발급은 실행하지 않습니다.</p>
    </header>
    <ActivityComparisonCard comparison={sample} />
  </main>;
}
