import type { NextConfig } from "next";

/**
 * MOBILE=1 يبني نسخة ثابتة (out/) تنحزم داخل تطبيق أندرويد عبر Capacitor.
 * ملفات .ts داخل app/ (مثل api/coach/route.ts) تُستثنى في هذا الوضع لأن
 * التصدير الثابت ما يدعم خادم؛ المدرب يتصل بخادم مستضاف لو انضبط
 * NEXT_PUBLIC_API_BASE، وإلا يشتغل وضعه المحلي.
 */
const mobile = process.env.MOBILE === "1";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  poweredByHeader: false,
  ...(mobile && {
    output: "export",
    trailingSlash: true,
    pageExtensions: ["tsx"],
    images: { unoptimized: true },
  }),
};

export default nextConfig;
