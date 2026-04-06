import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  allowedDevOrigins: ["172.30.1.32"],
  experimental: {
    proxyClientMaxBodySize: "5gb",
  },
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: "http://localhost:8080/api/:path*",
      },
      {
        source: "/hls/:path*",
        destination: "http://localhost:8080/hls/:path*",
      },
      {
        source: "/thumbnails/:path*",
        destination: "http://localhost:8080/thumbnails/:path*",
      },
    ];
  },
};

export default nextConfig;
