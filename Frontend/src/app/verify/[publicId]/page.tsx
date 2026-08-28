import { AppHeader } from "@/components/app-header";
import { VerificationClient } from "@/components/verification-client";

export default async function VerifyPage({
  params,
}: {
  params: Promise<{ publicId: string }>;
}) {
  const { publicId } = await params;
  return (
    <>
      <AppHeader />
      <main className="shell narrow-shell">
        <div className="page-header">
          <div>
            <p className="eyebrow">PUBLIC VERIFICATION</p>
            <h1>인증서 공개 검증</h1>
            <p className="muted">
              공개 ID와 Keccak-256 해시를 통해 Contribution Certificate의 진위 여부를 확인합니다.
            </p>
          </div>
        </div>
        <VerificationClient publicId={publicId} />
      </main>
    </>
  );
}
