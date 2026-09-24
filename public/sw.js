/* سند — Service Worker: يفتح التطبيق بدون إنترنت.
 * - صفحات: الشبكة أولاً، وإذا فشلت نرجع للنسخة المخزنة.
 * - ملفات Next الثابتة والخطوط: المخزن أولاً (أسماؤها فيها بصمة، لا تتغير).
 * - واجهة المدرب /api لا تُخزَّن أبداً.
 */
const VERSION = "sanad-v3";
const PAGES = ["/today", "/eat", "/coach", "/move", "/progress", "/start", "/manifest.webmanifest", "/icon.svg"];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches
      .open(VERSION)
      .then((c) => Promise.allSettled(PAGES.map((p) => c.add(new Request(p, { cache: "reload" })))))
      .then(() => self.skipWaiting()),
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== VERSION).map((k) => caches.delete(k))))
      .then(() => self.clients.claim()),
  );
});

self.addEventListener("fetch", (event) => {
  const req = event.request;
  if (req.method !== "GET") return;
  const url = new URL(req.url);
  if (url.origin !== self.location.origin || url.pathname.startsWith("/api/")) return;

  if (url.pathname.startsWith("/_next/static/") || /\.(woff2?|svg|png)$/.test(url.pathname)) {
    event.respondWith(
      caches.match(req).then(
        (hit) =>
          hit ||
          fetch(req).then((res) => {
            if (res.ok) {
              const copy = res.clone();
              caches.open(VERSION).then((c) => c.put(req, copy));
            }
            return res;
          }),
      ),
    );
    return;
  }

  if (req.mode === "navigate" || req.headers.get("accept")?.includes("text/html")) {
    event.respondWith(
      fetch(req)
        .then((res) => {
          if (res.ok) {
            const copy = res.clone();
            caches.open(VERSION).then((c) => c.put(url.pathname, copy));
          }
          return res;
        })
        .catch(async () => (await caches.match(url.pathname)) || (await caches.match("/today")) || Response.error()),
    );
  }
});

// الصفحة ترسل قائمة الملفات اللي حمّلتها قبل ما يتحكم الـ SW (خطوط، حزم JS)
self.addEventListener("message", (event) => {
  const data = event.data;
  if (!data || data.type !== "precache" || !Array.isArray(data.urls)) return;
  const urls = data.urls.filter((u) => typeof u === "string" && u.startsWith("/_next/static/")).slice(0, 200);
  event.waitUntil(
    caches.open(VERSION).then((c) =>
      Promise.allSettled(urls.map((u) => c.match(u).then((hit) => hit || c.add(u)))),
    ),
  );
});
