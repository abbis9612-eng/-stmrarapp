// لقطات شاشة للتحقق البصري: يمر على الإعداد ثم يزرع ٣ أسابيع بيانات واقعية.
// الاستخدام: npm run build && npx next start -p 3100 & ثم npm run shots
import { chromium } from "playwright-core";
import { mkdirSync } from "node:fs";

const BASE = process.env.BASE ?? "http://localhost:3100";
const OUT = "shots";
mkdirSync(OUT, { recursive: true });

const exe = process.env.CHROMIUM ?? "/opt/pw-browsers/chromium-1194/chrome-linux/chrome";
const browser = await chromium.launch({ executablePath: exe });
const errors = [];

function localKey(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function seed() {
  const today = new Date();
  const days = {};
  const skip = new Set([4, 11, 12]); // يوم مفرد ممسوك، ثم يومين = انقطاع قديم
  for (let i = 20; i >= 0; i--) {
    const d = new Date(today);
    d.setDate(today.getDate() - i);
    const key = localKey(d);
    const idx = 20 - i;
    if (skip.has(idx)) continue;
    const isToday = i === 0;
    days[key] = {
      date: key,
      energy: isToday ? null : ((idx % 3) + 1),
      time: isToday ? null : [2, 10, 20][idx % 3],
      meals: isToday
        ? []
        : [
            { id: `a${idx}`, name: "بيض مسلوق", kcal: 155, protein: 13, at: key, source: "db" },
            { id: `b${idx}`, name: "كبسة دجاج", kcal: 650, protein: 38, at: key, source: "coach" },
            { id: `c${idx}`, name: "زبادي يوناني", kcal: 100, protein: 17, at: key, source: "db" },
            { id: `d${idx}`, name: "شيش طاووق", kcal: 330, protein: 42, at: key, source: "db" },
            { id: `e${idx}`, name: "شاي كرك", kcal: 150, protein: 3, at: key, source: "db" },
          ],
      workouts: [],
      water: isToday ? 3 : 6,
      steps: 0,
      done: isToday ? [] : ["move", "eat"],
      weightKg: idx % 2 === 0 || idx > 17 ? Math.round((96 - idx * 0.09 + Math.sin(idx * 1.7) * 0.6) * 10) / 10 : null,
    };
  }
  const start = new Date(today);
  start.setDate(today.getDate() - 20);
  return {
    version: 1,
    profile: {
      name: "أبو فهد",
      sex: "m",
      age: 38,
      heightCm: 174,
      startWeightKg: 96,
      goalWeightKg: 84,
      activity: "sedentary",
      pace: "steady",
      why: "أتحرك بخفة مع عيالي",
      barriers: ["time", "night", "social"],
      ifThens: [
        { id: "1", when: "إذا جاني جوع بعد الساعة ٩", then: "أشرب شاي أو ماء، وإذا استمر آكل زبادي يوناني" },
        { id: "2", when: "إذا عندي عزيمة", then: "آكل بروتين خفيف قبلها وآخذ صحن واحد" },
      ],
      ramadan: false,
      flags: [],
      createdAt: localKey(start),
    },
    days,
    chat: [],
    favorites: ["greek-yogurt", "eggs-2"],
  };
}

async function run(scheme) {
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, colorScheme: scheme, locale: "ar-SA" });
  const page = await ctx.newPage();
  page.on("console", (m) => m.type() === "error" && !m.text().includes("503") && !m.text().includes("ERR_INTERNET_DISCONNECTED") && errors.push(`[${scheme}] ${m.text()}`));
  page.on("pageerror", (e) => errors.push(`[${scheme}] ${e.message}`));

  // ١) الإعداد من الصفر
  await page.goto(`${BASE}/`);
  await page.waitForURL("**/start");
  await page.screenshot({ path: `${OUT}/${scheme}-01-welcome.png`, fullPage: true });
  await page.getByRole("button", { name: /نبدأ/ }).click();
  await page.fill("#name", "سارة");
  await page.getByRole("button", { name: "أنثى" }).click();
  await page.fill("#age", "34");
  await page.fill("#h", "162");
  await page.fill("#w", "88");
  await page.screenshot({ path: `${OUT}/${scheme}-02-basics.png`, fullPage: true });
  await page.getByRole("button", { name: "التالي" }).click();
  await page.getByRole("button", { name: "خذ المحطة المقترحة" }).click();
  await page.screenshot({ path: `${OUT}/${scheme}-03-goal.png`, fullPage: true });
  await page.getByRole("button", { name: "التالي" }).click();
  await page.getByRole("button", { name: "صحتي وتحاليلي" }).click();
  await page.getByRole("button", { name: "أكل الليل" }).click();
  await page.getByRole("button", { name: "ما عندي وقت" }).click();
  await page.getByRole("button", { name: "التالي" }).click();
  await page.getByRole("button", { name: "اعرض خطتي" }).click();
  await page.screenshot({ path: `${OUT}/${scheme}-04-plan.png`, fullPage: true });
  await page.getByRole("button", { name: "ابدأ يومي الأول" }).click();
  await page.waitForURL("**/today");
  await page.screenshot({ path: `${OUT}/${scheme}-05-checkin.png`, fullPage: true });
  await page.getByRole("button", { name: "طاقتي تحت" }).click();
  await page.getByRole("button", { name: "دقيقتين" }).click();
  await page.getByRole("button", { name: "فصّل لي خطة اليوم" }).click();
  await page.screenshot({ path: `${OUT}/${scheme}-06-today-low.png`, fullPage: true });

  // ٢) بيانات ٣ أسابيع
  await page.evaluate((s) => localStorage.setItem("sanad:v1", JSON.stringify(s)), seed());
  await page.goto(`${BASE}/today`);
  await page.getByRole("button", { name: "فل طاقة" }).click();
  await page.getByRole("button", { name: "٢٠ دقيقة" }).click();
  await page.getByRole("button", { name: "فصّل لي خطة اليوم" }).click();
  await page.getByRole("button", { name: /أنجزت: سجّل وجباتك/ }).click();
  await page.screenshot({ path: `${OUT}/${scheme}-07-today-high.png`, fullPage: true });

  await page.goto(`${BASE}/eat`);
  await page.fill("#q", "كبسه");
  await page.getByRole("button", { name: /كبسة دجاج/ }).first().click();
  await page.screenshot({ path: `${OUT}/${scheme}-08-eat.png`, fullPage: true });
  await page.getByRole("button", { name: /^حصة \d|^حصة [٠-٩]/ }).click();

  await page.goto(`${BASE}/coach`);
  await page.getByRole("button", { name: "تغديت كبسة دجاج ولبن" }).click();
  await page.getByText(/حسبتها|سعرة/).first().waitFor({ timeout: 15000 });
  await page.screenshot({ path: `${OUT}/${scheme}-09-coach.png`, fullPage: true });

  await page.goto(`${BASE}/move`);
  await page.screenshot({ path: `${OUT}/${scheme}-10-move.png`, fullPage: true });
  await page.goto(`${BASE}/move?play=strength-10`);
  await page.waitForTimeout(1500);
  await page.screenshot({ path: `${OUT}/${scheme}-11-player.png` });

  await page.goto(`${BASE}/progress`);
  await page.screenshot({ path: `${OUT}/${scheme}-12-progress.png`, fullPage: true });

  // التذكير: ينزّل ملف تقويم صالح
  const [download] = await Promise.all([page.waitForEvent("download"), page.getByRole("button", { name: "أضف التذكيرات لتقويمي" }).click()]);
  const icsPath = await download.path();
  const ics = (await import("node:fs")).readFileSync(icsPath, "utf8");
  if (!ics.includes("RRULE:FREQ=DAILY") || (ics.match(/BEGIN:VEVENT/g) ?? []).length !== 2) errors.push(`[${scheme}] bad ics download`);

  // بدون إنترنت: الصفحات تفتح من الـ Service Worker، والمدرب المحلي يرد
  await page.evaluate(() => navigator.serviceWorker.ready.then(() => true));
  for (const path of ["/today", "/eat", "/coach", "/move", "/progress"]) await page.goto(`${BASE}${path}`);
  await ctx.setOffline(true);
  const offlineFail = (r) => r.url().includes("/_next/static/") && errors.push(`[${scheme}] offline asset missing: ${r.url()}`);
  page.on("requestfailed", offlineFail);
  await page.goto(`${BASE}/eat`);
  if (!(await page.getByRole("heading", { name: "الأكل" }).isVisible())) errors.push(`[${scheme}] /eat not available offline`);
  await page.goto(`${BASE}/coach`);
  await page.getByRole("status").filter({ hasText: "بدون إنترنت" }).waitFor({ timeout: 5000 });
  await page.fill("#msg", "فطرت بيضتين وشاي كرك");
  await page.keyboard.press("Enter");
  await page.getByText("وضع محلي").last().waitFor({ timeout: 5000 });
  await page.screenshot({ path: `${OUT}/${scheme}-13-offline-coach.png`, fullPage: true });
  page.off("requestfailed", offlineFail);
  await ctx.setOffline(false);

  // فحص: لا تمرير أفقي
  for (const path of ["/today", "/eat", "/coach", "/move", "/progress"]) {
    await page.goto(`${BASE}${path}`);
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
    if (overflow > 1) errors.push(`[${scheme}] horizontal overflow on ${path}: ${overflow}px`);
  }
  await ctx.close();
}

try {
  await run("light");
  await run("dark");
} finally {
  await browser.close();
}
if (errors.length) {
  console.error("ERRORS:\n" + errors.join("\n"));
  process.exit(1);
}
console.log("screenshots ok");
