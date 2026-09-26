// ينسخ تعليمات المدرب ومخطط الرد من كود أندرويد (مصدر الحقيقة) إلى السيرفر،
// حتى المدرب السحابي وتطبيق "مفتاحك الخاص" يتصرفون بنفس الشكل.
// الاستخدام: node scripts/sync-coach-prompt.mjs        (يكتب الملف)
//            node scripts/sync-coach-prompt.mjs --check (يفشل إذا الملف قديم)
import { readFileSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

const root = new URL("../", import.meta.url);
const KOTLIN = fileURLToPath(new URL("android/core/src/main/kotlin/app/sanad/core/ai/CoachPrompt.kt", root));
const OUT = fileURLToPath(new URL("src/lib/coach-prompt.generated.ts", root));

export function extract(src, name) {
  const m = src.match(new RegExp(`const val ${name}: String = """([\\s\\S]*?)"""`));
  if (!m) throw new Error(`${name} not found in CoachPrompt.kt`);
  if (m[1].includes("$")) throw new Error(`${name} uses Kotlin string templates; keep it a plain literal`);
  return m[1];
}

export function render(kotlinSrc) {
  const system = extract(kotlinSrc, "COACH_SYSTEM");
  const schema = JSON.parse(extract(kotlinSrc, "COACH_SCHEMA_JSON"));
  return (
    "// مولَّد من android/core/.../ai/CoachPrompt.kt عبر scripts/sync-coach-prompt.mjs — لا تعدّله يدوياً.\n" +
    `export const COACH_SYSTEM = ${JSON.stringify(system)};\n\n` +
    `export const COACH_SCHEMA = ${JSON.stringify(schema, null, 2)} as const;\n`
  );
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const next = render(readFileSync(KOTLIN, "utf8"));
  if (process.argv.includes("--check")) {
    const cur = readFileSync(OUT, "utf8");
    if (cur !== next) {
      console.error("coach-prompt.generated.ts is stale: run node scripts/sync-coach-prompt.mjs");
      process.exit(1);
    }
  } else {
    writeFileSync(OUT, next);
    console.log("wrote", OUT);
  }
}
