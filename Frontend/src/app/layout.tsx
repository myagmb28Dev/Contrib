import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Contribution Attestation",
  description: "GitHub 활동 기반 검증 가능한 Contribution Certificate",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="ko">
      <body>{children}</body>
    </html>
  );
}

