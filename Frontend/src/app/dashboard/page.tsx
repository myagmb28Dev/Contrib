import { AppHeader } from "@/components/app-header";
import { DashboardClient } from "./dashboard-client";

export default function DashboardPage() {
  return (
    <>
      <AppHeader />
      <main className="shell">
        <div className="page-header">
          <h1>Contrib</h1>
        </div>
        <DashboardClient />
      </main>
    </>
  );
}
