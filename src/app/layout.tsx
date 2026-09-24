import type { Metadata, Viewport } from "next";
import "./globals.css";
import { RegisterSW } from "@/components/RegisterSW";

export const metadata: Metadata = {
  title: "سَنَد — مدربك للتنحيف",
  description: "مدرب تنحيف ذكي يتكيّف مع طاقتك ووقتك. خطوة صغيرة كل يوم، بدون يوم صفر.",
  applicationName: "سند",
  appleWebApp: { capable: true, title: "سند", statusBarStyle: "default" },
  manifest: "/manifest.webmanifest",
  icons: { icon: "/icon.svg", apple: "/icon.svg" },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#f1f3f7" },
    { media: "(prefers-color-scheme: dark)", color: "#0c1424" },
  ],
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ar" dir="rtl">
      <body>
        {children}
        <RegisterSW />
      </body>
    </html>
  );
}
