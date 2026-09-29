import { apiBaseUrl, type ActivityComparison } from "@/lib/api";

const labels: Record<string, string> = {
  commits: "커밋", pullRequestsOpened: "생성한 PR", reviews: "코드 리뷰", activeDays: "활동 일수",
};

export function ComparisonSummary({ comparison }: { comparison?: ActivityComparison | null }) {
  return <span>{comparison?.status === "AVAILABLE" && comparison.percentile != null
    ? `상대 활동 ${comparison.percentile.toFixed(1)}백분위`
    : comparison ? "백분위 산정 보류" : "이전 분석 · 백분위 없음"}</span>;
}

export function ActivityComparisonCard({ comparison }: { comparison?: ActivityComparison | null }) {
  if (!comparison) return <section id="activity-comparison" className="card stack" aria-label="상대 활동 수준">
    <h3>상대 활동 수준 · 실험적 백분위</h3>
    <p>이전 분석에는 비교 데이터가 없습니다. 새 분석을 실행하면 비교 가능 여부를 확인할 수 있습니다.</p>
    <p className="muted">이전 분석과 발급된 인증서 기록은 그대로 보존됩니다.</p>
  </section>;
  const available = comparison.status === "AVAILABLE" && comparison.percentile != null;
  return <section id="activity-comparison" className="card stack" aria-label="상대 활동 수준">
    <h3>상대 활동 수준 · 실험적 백분위</h3>
    {available ? <div>
      <strong className="comparison-percentile">{comparison.percentile!.toFixed(1)}</strong> 백분위
      <p>선택한 비교 집단에서 상위 약 {(100 - comparison.percentile!).toFixed(1)}%에 해당하는 활동 수준입니다.</p>
    </div> : <strong>백분위 산정 보류</strong>}
    <p>{comparison.reason}</p>
    {available && (comparison.singleActivitySensitivity ?? 0) >= 20 && <p role="note" className="comparison-caution">
      참고: 이 비교 집단에서는 커밋·PR·리뷰 중 한 항목이 1건 증가해도
      최대 {comparison.singleActivitySensitivity!.toFixed(1)}백분위 포인트가 달라질 수 있습니다.
      표본과 동점의 영향을 크게 받으므로 작은 수치 차이를 과해석하지 마세요.
    </p>}
    <dl className="identity-list">
      <div><dt>분석 기간 (UTC · 종료 제외)</dt><dd>{comparison.periodStart} ~ {comparison.periodEnd}</dd></div>
      <div><dt>비교 조건</dt><dd>{comparison.language ?? "언어 미확인"} · 활동 기여자 {comparison.sizeBand}명 규모의 공개 저장소</dd></div>
      <div><dt>비교 표본</dt><dd>{comparison.repositories.length}개 저장소 · {comparison.contributorCount}명 · 기여자와 저장소 조합 {comparison.sampleCount}건</dd></div>
    </dl>
    {available && <div className="compact-table-container"><table className="compact-table">
      <thead><tr><th>활동 항목</th><th>백분위</th><th>종합 반영 비중</th></tr></thead>
      <tbody>{Object.entries(comparison.metricPercentiles).map(([key, value]) => <tr key={key}>
        <td>{labels[key] ?? key}</td><td>{value.toFixed(1)}</td>
        <td>{((comparison.metricWeights[key] ?? 0) * 100).toFixed(0)}%</td>
      </tr>)}</tbody>
    </table></div>}
    <p className="muted">개발 실력·품질·생산성의 평가가 아닙니다. 100백분위도 완벽한 기여를 뜻하지 않습니다.
      동일 인물의 여러 저장소 활동은 합계 가중치 1로 처리하며, 동점은 중간 순위를 사용합니다.</p>
    <p className="muted">리뷰는 이 기간에 생성된 PR에 제출한 타인 리뷰만 포함합니다. 기간 이전에 생성된 PR의 리뷰는 이 모델의 수집 범위에 포함되지 않습니다.</p>
    <details>
      <summary>산정 기준과 비교 데이터 확인</summary>
      <p>모델: {comparison.modelVersion}</p>
      <p>최소 5명이며 비교 기여자의 20% 이상이 활동한 항목만 같은 비중으로 반영합니다.
        비교 가능한 항목이 2개 미만이면 산정을 보류합니다. PR 병합과 변경 파일 수는 별도 가산하지 않습니다.</p>
      <p>비교 저장소: {comparison.repositories.join(", ") || "없음"}</p>
      <p>기준 데이터 수집 시각: {comparison.referenceCollectedAt ?? "없음"}</p>
      <p className="comparison-hash">데이터 버전: {comparison.datasetId ?? "없음"}</p>
      <p className="comparison-hash">비교 집단 식별자: {comparison.cohortId ?? "없음"}</p>
      <p>{comparison.scope}</p><p>{comparison.calculationRules}</p>
      {comparison.datasetId && <a href={`${apiBaseUrl}/api/public/benchmarks/${encodeURIComponent(comparison.datasetId)}`} target="_blank" rel="noreferrer">사용된 공개 비교 데이터 보기 (JSON)</a>}
    </details>
  </section>;
}
