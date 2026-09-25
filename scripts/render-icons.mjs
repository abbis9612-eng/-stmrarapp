// يولّد صور المصدر لأيقونات أندرويد من شعار السدو (public/icon.svg)
import { chromium } from "playwright-core";
import { readFileSync } from "node:fs";

const svg = readFileSync("public/icon.svg", "utf8");
const glyph = svg.replace(/<rect width="512" height="512" rx="112" fill="#14213d"\/>/, "");
const exe = process.env.CHROMIUM ?? "/opt/pw-browsers/chromium-1194/chrome-linux/chrome";
const browser = await chromium.launch({ executablePath: exe });
const page = await browser.newPage();

async function shot(file, size, html) {
  await page.setViewportSize({ width: size, height: size });
  await page.setContent(`<html><body style="margin:0;background:transparent">${html}</body></html>`);
  await page.screenshot({ path: `assets/${file}`, omitBackground: true });
}

const svgAt = (s, px) => s.replace("<svg ", `<svg width="${px}" height="${px}" `);
await shot("icon-only.png", 1024, svgAt(svg, 1024));
await shot("icon-background.png", 1024, `<div style="width:1024px;height:1024px;background:#14213d"></div>`);
// المنطقة الآمنة للأيقونة التكيّفية ~٦٦٪
await shot("icon-foreground.png", 1024, `<div style="width:1024px;height:1024px;display:grid;place-items:center">${svgAt(glyph.replace('viewBox="0 0 512 512"', 'viewBox="56 96 400 320"'), 640)}</div>`);
for (const [file, bg] of [["splash.png", "#f1f3f7"], ["splash-dark.png", "#0c1424"]]) {
  await shot(file, 2732, `<div style="width:2732px;height:2732px;background:${bg};display:grid;place-items:center">${svgAt(svg, 560)}</div>`);
}
await browser.close();
console.log("icons rendered");
