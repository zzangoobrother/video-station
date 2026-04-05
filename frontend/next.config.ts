import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
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
