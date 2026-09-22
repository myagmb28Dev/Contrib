import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  agentRules: false,
  output: "standalone",
  poweredByHeader: false,
  async rewrites() {
    const upstream = process.env.API_PROXY_TARGET?.replace(/\/$/, "");
    if (!upstream) return [];
    if (!/^https?:\/\//.test(upstream)) {
      throw new Error("API_PROXY_TARGET must be an absolute HTTP(S) URL");
    }
    return [
      { source: "/api/:path*", destination: `${upstream}/api/:path*` },
      { source: "/oauth2/:path*", destination: `${upstream}/oauth2/:path*` },
    ];
  },
};

export default nextConfig;
