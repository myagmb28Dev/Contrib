import { AnalysesClient } from "@/components/analyses-client";
import { AppNav } from "@/components/app-nav";

export default function AnalysesPage() {
  return <main className="shell"><p className="eyebrow">Contribution Attestation</p><AppNav />
    <div className="card stack"><h1>Analyses</h1><AnalysesClient /></div></main>;
}
