"use client";

import { useEffect } from "react";

export function RegisterSW() {
  useEffect(() => {
    if (process.env.NODE_ENV !== "production" || !("serviceWorker" in navigator)) return;
    navigator.serviceWorker
      .register("/sw.js")
      .then(() => navigator.serviceWorker.ready)
      .then((reg) => {
        const urls = performance
          .getEntriesByType("resource")
          .map((e) => new URL(e.name))
          .filter((u) => u.origin === location.origin)
          .map((u) => u.pathname);
        reg.active?.postMessage({ type: "precache", urls });
      })
      .catch(() => {
      /* المتصفح يرفض (وضع خاص مثلاً) — التطبيق يشتغل عادي أونلاين */
    });
  }, []);
  return null;
}
