"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useApp } from "@/lib/store";

export default function Home() {
  const s = useApp();
  const router = useRouter();
  useEffect(() => {
    // داخل تطبيق أندرويد، Capacitor يرجّع هذي الصفحة لأي مسار بدون امتداد
    // (مثل /eat/ عند إعادة التحميل). نفتح ملف الصفحة الحقيقي بدل ما نرجع لليوم.
    const path = window.location.pathname;
    if (path !== "/" && !path.endsWith("/index.html")) {
      window.location.replace(`${path.replace(/\/?$/, "/")}index.html${window.location.search}`);
      return;
    }
    if (s.ready) router.replace(s.profile ? "/today" : "/start");
  }, [s.ready, s.profile, router]);
  return <main className="shell shell--bare" aria-busy="true" />;
}
