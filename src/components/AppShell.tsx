"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useApp } from "@/lib/store";
import { Nav } from "./Nav";

/** يغلّف الصفحات الرئيسية: يحوّل للإعداد إذا ما في ملف شخصي. */
export function AppShell({ children }: { children: React.ReactNode }) {
  const s = useApp();
  const router = useRouter();
  useEffect(() => {
    if (s.ready && !s.profile) router.replace("/start");
  }, [s.ready, s.profile, router]);

  if (!s.ready || !s.profile) return <main className="shell" aria-busy="true" />;
  return (
    <>
      <main className="shell">{children}</main>
      <Nav />
    </>
  );
}
