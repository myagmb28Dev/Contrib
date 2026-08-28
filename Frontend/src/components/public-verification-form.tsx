"use client";

import { useRouter } from "next/navigation";
import { type FormEvent, useState } from "react";
import { TypewriterText } from "./typewriter";

export function PublicVerificationForm() {
  const router = useRouter();
  const [publicId, setPublicId] = useState("");

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedId = publicId.trim();
    if (normalizedId) {
      router.push(`/verify/${encodeURIComponent(normalizedId)}`);
    }
  }

  return (
    <section className="verify-section" id="verify">
      <div>
        <p className="eyebrow">Public verification</p>
        <h2>받은 인증서의 유효성을 확인하세요.</h2>
        <p style={{ minHeight: "48px" }}>
          <TypewriterText text={"로그인 없이 공개 ID만 입력하면 발급·폐기 상태와\nHash 일치 여부를 확인할 수 있습니다."} />
        </p>
      </div>
      <form className="verification-form" onSubmit={handleSubmit}>
        <label htmlFor="public-certificate-id">Certificate public ID</label>
        <div>
          <input
            id="public-certificate-id"
            name="publicId"
            value={publicId}
            onChange={(event) => setPublicId(event.target.value)}
            placeholder="예: 23f1b7ec-..."
            autoComplete="off"
            required
          />
          <button className="button primary" type="submit">
            검증하기
          </button>
        </div>
      </form>
    </section>
  );
}
