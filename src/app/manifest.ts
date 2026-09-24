import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "سَنَد — مدربك للتنحيف",
    short_name: "سند",
    description: "مدرب تنحيف ذكي يتكيّف مع طاقتك ووقتك.",
    start_url: "/today",
    display: "standalone",
    dir: "rtl",
    lang: "ar",
    background_color: "#f1f3f7",
    theme_color: "#14213d",
    icons: [{ src: "/icon.svg", sizes: "any", type: "image/svg+xml", purpose: "any" }],
  };
}
