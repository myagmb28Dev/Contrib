import { AppNav } from "@/components/app-nav";
import { RepositoriesClient } from "@/components/repositories-client";

export default function RepositoriesPage() {
  return <main className="shell"><p className="eyebrow">Contribution Attestation</p><AppNav />
    <div className="card stack"><h1>Repositories</h1><p className="muted">GitHub 공개 저장소를 동기화하고 분석할 저장소를 선택하세요.</p><RepositoriesClient /></div>
  </main>;
}
