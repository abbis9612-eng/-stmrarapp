import { n } from "./format";
import type { Activity, DayLog, Energy, Pace, Profile, Sex, TimeBudget } from "./types";

/** طاقة تقريبية لكل كغ من نسيج الجسم المفقود (خليط دهون وعضل). */
export const KCAL_PER_KG = 7700;

const ACTIVITY_FACTOR: Record<Activity, number> = {
  sedentary: 1.2,
  light: 1.375,
  moderate: 1.55,
  active: 1.725,
};

/** نسبة النزول الأسبوعي من وزن الجسم. أقصى حد آمن ~1٪ أسبوعياً. */
const PACE_RATE: Record<Pace, number> = {
  gentle: 0.0035,
  steady: 0.006,
  brisk: 0.009,
};

export const CALORIE_FLOOR: Record<Sex, number> = { f: 1200, m: 1500 };

const round = (n: number, step = 1) => Math.round(n / step) * step;

export function bmi(weightKg: number, heightCm: number): number {
  const h = heightCm / 100;
  return weightKg / (h * h);
}

/** معادلة Mifflin-St Jeor — الأدق بين المعادلات الشائعة للبالغين. */
export function bmr(sex: Sex, weightKg: number, heightCm: number, age: number): number {
  const base = 10 * weightKg + 6.25 * heightCm - 5 * age;
  return sex === "m" ? base + 5 : base - 161;
}

export function formulaTdee(p: Pick<Profile, "sex" | "age" | "heightCm" | "activity">, weightKg: number): number {
  return bmr(p.sex, weightKg, p.heightCm, p.age) * ACTIVITY_FACTOR[p.activity];
}

/**
 * وزن مرجعي للبروتين: عند زيادة الوزن نستخدم وزناً معدّلاً
 * (الوزن عند BMI 25 + ربع الفائض) حتى لا يتضخم الهدف بلا داعٍ.
 */
export function referenceWeight(weightKg: number, heightCm: number): number {
  const h = heightCm / 100;
  const at25 = 25 * h * h;
  if (weightKg <= at25) return weightKg;
  return at25 + 0.25 * (weightKg - at25);
}

/** 1.2–1.6 غ/كغ أثناء النزول للحفاظ على العضل؛ نستهدف 1.5 من الوزن المرجعي. */
export function proteinTarget(weightKg: number, heightCm: number): number {
  return round(1.5 * referenceWeight(weightKg, heightCm), 5);
}

export function weeklyLossKg(pace: Pace, weightKg: number): number {
  return PACE_RATE[pace] * weightKg;
}

export interface Targets {
  tdee: number;
  kcal: number;
  protein: number;
  weeklyLossKg: number;
  steps: number;
  water: number;
  floorApplied: boolean;
}

export function computeTargets(profile: Profile, weightKg: number, tdeeOverride?: number): Targets {
  const tdee = tdeeOverride ?? formulaTdee(profile, weightKg);
  const loss = weeklyLossKg(profile.pace, weightKg);
  const deficit = (loss * KCAL_PER_KG) / 7;
  const raw = tdee - deficit;
  const floor = CALORIE_FLOOR[profile.sex];
  const kcal = Math.max(raw, floor);
  const baseSteps = { sedentary: 6000, light: 7000, moderate: 8000, active: 9000 }[profile.activity];
  return {
    tdee: round(tdee, 10),
    kcal: round(kcal, 10),
    protein: proteinTarget(weightKg, profile.heightCm),
    weeklyLossKg: Math.round(loss * 100) / 100,
    steps: baseSteps,
    water: 8,
    floorApplied: raw < floor,
  };
}

/* ------------------------------------------------------------------ */
/* الوزن الاتجاهي: متوسط متحرك أُسّي يخفي تذبذب الماء والملح           */
/* ------------------------------------------------------------------ */

export interface WeightPoint {
  date: string;
  kg: number;
}

export interface TrendPoint extends WeightPoint {
  trend: number;
}

export function daysBetween(a: string, b: string): number {
  const ms = Date.parse(`${b}T00:00:00Z`) - Date.parse(`${a}T00:00:00Z`);
  return Math.round(ms / 86_400_000);
}

/**
 * EMA بمعامل 0.1 يومياً، مع مراعاة الأيام الفارغة:
 * كلما طالت الفجوة زاد وزن القراءة الجديدة.
 */
export function trendWeights(points: WeightPoint[], alpha = 0.1): TrendPoint[] {
  const sorted = [...points].sort((a, b) => a.date.localeCompare(b.date));
  const out: TrendPoint[] = [];
  let trend: number | null = null;
  let last: string | null = null;
  for (const p of sorted) {
    if (trend === null || last === null) {
      trend = p.kg;
    } else {
      const gap = Math.max(1, daysBetween(last, p.date));
      const a = 1 - Math.pow(1 - alpha, gap);
      trend = trend + a * (p.kg - trend);
    }
    last = p.date;
    out.push({ ...p, trend: Math.round(trend * 100) / 100 });
  }
  return out;
}

/* ------------------------------------------------------------------ */
/* الحرق التكيّفي: نتعلم حرقك الحقيقي من أكلك المسجّل وتغيّر وزنك      */
/* ------------------------------------------------------------------ */

export interface AdaptiveResult {
  tdee: number;
  confidence: number;
  loggedDays: number;
  windowDays: number;
}

export function dayIntake(day: DayLog): number {
  return day.meals.reduce((s, m) => s + m.kcal, 0);
}

/**
 * نافذة حتى 21 يوماً: الحرق ≈ متوسط الأكل − (تغيّر الوزن الاتجاهي × 7700 ÷ الأيام).
 * نمزجه مع تقدير المعادلة بحسب كمية البيانات، ونحدّ التغيير بـ ±25٪.
 */
export function adaptiveTdee(profile: Profile, days: DayLog[], today: string): AdaptiveResult | null {
  const window = days
    .filter((d) => daysBetween(d.date, today) >= 0 && daysBetween(d.date, today) < 21)
    .sort((a, b) => a.date.localeCompare(b.date));
  const intakeDays = window.filter((d) => dayIntake(d) >= 600);
  const weights = window.filter((d) => d.weightKg !== null).map((d) => ({ date: d.date, kg: d.weightKg as number }));
  if (intakeDays.length < 7 || weights.length < 3) return null;

  const trend = trendWeights(weights);
  const first = trend[0];
  const lastT = trend[trend.length - 1];
  const span = daysBetween(first.date, lastT.date);
  if (span < 7) return null;

  const avgIntake = intakeDays.reduce((s, d) => s + dayIntake(d), 0) / intakeDays.length;
  const measured = avgIntake - ((lastT.trend - first.trend) * KCAL_PER_KG) / span;
  const formula = formulaTdee(profile, lastT.trend);
  const confidence = Math.min(1, (intakeDays.length / 14) * Math.min(1, span / 14));
  const blended = formula + (measured - formula) * confidence;
  const clamped = Math.min(formula * 1.25, Math.max(formula * 0.75, blended));
  return {
    tdee: round(clamped, 10),
    confidence: Math.round(confidence * 100) / 100,
    loggedDays: intakeDays.length,
    windowDays: span,
  };
}

/* ------------------------------------------------------------------ */
/* الخيط: لا تفوّت مرتين — اليوم الواحد الضائع لا يقطع السلسلة         */
/* ------------------------------------------------------------------ */

export function isCounted(day: DayLog | undefined): boolean {
  if (!day) return false;
  return day.done.length > 0 || day.meals.length > 0 || day.workouts.length > 0 || day.weightKg !== null;
}

export interface ThreadState {
  length: number;
  /** أمس ضاع: اليوم إنقاذ للخيط */
  rescueToday: boolean;
  best: number;
}

export function addDays(date: string, n: number): string {
  const d = new Date(`${date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + n);
  return d.toISOString().slice(0, 10);
}

export function computeThread(days: Record<string, DayLog>, today: string): ThreadState {
  const keys = Object.keys(days).sort();
  if (keys.length === 0) return { length: 0, rescueToday: false, best: 0 };

  let best = 0;
  let run = 0;
  let misses = 0;
  let cursor = keys[0];
  while (cursor <= today) {
    if (isCounted(days[cursor])) {
      run += 1;
      misses = 0;
    } else if (cursor !== today) {
      misses += 1;
      if (misses >= 2) run = 0;
    }
    best = Math.max(best, run);
    cursor = addDays(cursor, 1);
  }
  const yesterday = addDays(today, -1);
  const rescueToday = run > 0 && !isCounted(days[yesterday]) && !isCounted(days[today]) && keys[0] <= yesterday;
  return { length: run, rescueToday, best };
}

/* ------------------------------------------------------------------ */
/* خطة اليوم: تتقلص وتكبر حسب طاقتك ووقتك — لا يوجد يوم صفر          */
/* ------------------------------------------------------------------ */

export interface Mission {
  id: string;
  kind: "move" | "eat" | "restore";
  title: string;
  detail: string;
  routineId?: string;
}

export function dayMissions(energy: Energy, time: TimeBudget, t: Targets, profile: Profile): Mission[] {
  const proteinMeal = Math.round(t.protein / 3 / 5) * 5;
  const move: Mission =
    energy === 1 || time === 2
      ? {
          id: "move",
          kind: "move",
          title: "دقيقتين حركة فقط",
          detail: "٣ تمارين هادئة وأنت بمكانك. الهدف تحافظ على الخيط، مو تتعب.",
          routineId: energy === 1 ? "reset-2" : "wake-2",
        }
      : time === 10
        ? {
            id: "move",
            kind: "move",
            title: energy === 3 ? "١٠ دقائق قوة للجسم كامل" : "١٠ دقائق حركة بدون قفز",
            detail: "تمارين بوزن الجسم تحمي عضلاتك وأنت تنزل وزن.",
            routineId: energy === 3 ? "strength-10" : "low-impact-10",
          }
        : {
            id: "move",
            kind: "move",
            title: energy === 3 ? "٢٠ دقيقة قوة + مشي" : "٢٠ دقيقة مشي خفيف",
            detail: energy === 3 ? "جلسة قوة كاملة؛ أهم استثمار للحفاظ على العضل." : `امشِ بإيقاع مريح. هدف خطواتك ${n(t.steps)}.`,
            routineId: energy === 3 ? "strength-20" : "walk-20",
          };

  const eat: Mission =
    energy === 1
      ? {
          id: "eat",
          kind: "eat",
          title: "ابدأ وجبتك الجاية بالبروتين",
          detail: `حوالي ${n(proteinMeal)} غ (بيض، زبادي، دجاج، تونة). التعب يرفع الجوع — البروتين يهدّيه.`,
        }
      : {
          id: "eat",
          kind: "eat",
          title: "سجّل وجباتك — ولو بجملة",
          detail: `قل لسند "تغديت كبسة دجاج" ويحسبها. هدفك ${n(t.protein)} غ بروتين اليوم.`,
        };

  const nightBarrier = profile.barriers.includes("night");
  const restore: Mission =
    energy === 1
      ? {
          id: "restore",
          kind: "restore",
          title: "نوم أبكر بنص ساعة",
          detail: "قلة النوم ترفع هرمون الجوع وتضعف الإرادة. الليلة استثمار.",
        }
      : nightBarrier
        ? {
            id: "restore",
            kind: "restore",
            title: "المطبخ يسكّر الساعة ٩",
            detail: "بعدها شاي أو ماء فقط. خطتك لو جاك جوع الليل جاهزة في صفحة المدرب.",
          }
        : {
            id: "restore",
            kind: "restore",
            title: "٨ أكواب ماء",
            detail: "ابدأ بكوب قبل كل وجبة؛ يساعد على الشبع ويقلل الخلط بين العطش والجوع.",
          };

  return [move, eat, restore];
}

export const ENERGY_COPY: Record<Energy, { label: string; line: string }> = {
  1: { label: "طاقتي تحت", line: "عادي. اليوم نحافظ على الخيط بأصغر خطوة ممكنة." },
  2: { label: "نص نص", line: "يوم متوازن: خطوات ثابتة بدون ضغط." },
  3: { label: "فل طاقة", line: "استغلها! اليوم نبني عضل ونسبق الخطة." },
};
