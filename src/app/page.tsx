"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useApp } from "@/lib/store";

export default function Home() {
  const s = useApp();
  const router = useRouter();
  useEffect(() => {
    if (s.ready) router.replace(s.profile ? "/today" : "/start");
  }, [s.ready, s.profile, router]);
  return <main className="shell shell--bare" aria-busy="true" />;
}
