// خادم يحاكي WebViewLocalServer في Capacitor (html5mode):
// أي مسار بدون امتداد يرجع index.html الجذر، والباقي ملفات من out/.
import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { extname, join, normalize } from "node:path";

const root = "out";
const types = { ".html": "text/html; charset=utf-8", ".js": "text/javascript", ".css": "text/css", ".txt": "text/plain; charset=utf-8", ".svg": "image/svg+xml", ".woff2": "font/woff2", ".json": "application/json", ".webmanifest": "application/manifest+json" };
const port = Number(process.env.PORT ?? 3200);

createServer(async (req, res) => {
  const url = new URL(req.url, "http://x");
  const last = url.pathname.split("/").filter(Boolean).pop() ?? "";
  const path = url.pathname === "/" || !last.includes(".") ? "/index.html" : url.pathname;
  try {
    const file = join(root, normalize(decodeURIComponent(path)).replace(/^(\.\.[/\\])+/, ""));
    const body = await readFile(file);
    res.writeHead(200, { "content-type": types[extname(file)] ?? "application/octet-stream" });
    res.end(body);
  } catch {
    res.writeHead(404);
    res.end();
  }
}).listen(port, () => console.log(`capacitor-like server on :${port}`));
