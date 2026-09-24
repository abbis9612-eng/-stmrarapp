"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Icon } from "./Icon";

const TABS = [
  { href: "/today", label: "اليوم", icon: "today" },
  { href: "/eat", label: "الأكل", icon: "eat" },
  { href: "/coach", label: "سند", icon: "coach" },
  { href: "/move", label: "الحركة", icon: "move" },
  { href: "/progress", label: "التقدّم", icon: "progress" },
] as const;

export function Nav() {
  const path = usePathname();
  return (
    <nav className="nav" aria-label="التنقل الرئيسي">
      <ul>
        {TABS.map((t) => {
          const active = path.startsWith(t.href);
          return (
            <li key={t.href}>
              <Link href={t.href} aria-current={active ? "page" : undefined}>
                {t.icon === "coach" ? (
                  <span className="nav-coach">
                    <Icon name="coach" size={26} />
                  </span>
                ) : (
                  <Icon name={t.icon} />
                )}
                <span>{t.label}</span>
                <span className="nav-dot" />
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
