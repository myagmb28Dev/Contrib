import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Contrib - GitHub 기여도 증명 및 온체인 인증서",
  description: "GitHub 활동 기반 검증 가능한 Contribution Certificate",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="ko" suppressHydrationWarning>
      <body suppressHydrationWarning>{children}</body>
    </html>
  );
}
