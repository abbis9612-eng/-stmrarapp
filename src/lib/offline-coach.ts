import { parseMealText } from "./foods";
import type { AppState, CoachAction } from "./types";
import { dayIntake, trendWeights, type Targets } from "./science";
import { todayKey } from "./store";
import { n } from "./format";

/**
 * مدرب محلي يعمل بلا إنترنت أو بلا مفتاح API. قواعد مبنية على نفس
 * مبادئ المدرب الذكي، حتى لا يبقى المستخدم بدون سند أبداً.
 */
export function offlineReply(text: string, s: AppState, t: Targets): { reply: string; actions: CoachAction[] } {
  const q = text.replace(/[أإآ]/g, "ا").replace(/ة/g, "ه");
  const today = s.days[todayKey()];
  const eaten = today ? dayIntake(today) : 0;
  const proteinIn = today ? today.meals.reduce((a, m) => a + m.protein, 0) : 0;
  const left = t.kcal - eaten;

  const water = q.match(/(\d+|كوب|كوبين|قلاص|ماي|ماء|مويه)/);
  if (/(شربت|اشرب).*(ماء|ماي|مويه)/.test(q) && water) {
    const n = /كوبين/.test(q) ? 2 : Number(q.match(/\d+/)?.[0] ?? 1);
    return { reply: `ممتاز 💧 سجّلها وكمّل. الماء قبل الوجبة يساعد على الشبع.`, actions: [{ type: "log_water", cups: Math.min(10, n) }] };
  }

  const items = parseMealText(text);
  if (items.length) {
    const actions: CoachAction[] = items.map(({ food, qty }) => ({
      type: "log_meal",
      name: qty === 1 ? food.name : `${food.name} ×${n(qty)}`,
      kcal: Math.round(food.kcal * qty),
      protein: Math.round(food.protein * qty),
    }));
    const kcal = actions.reduce((a, x) => a + (x.type === "log_meal" ? x.kcal : 0), 0);
    const prot = actions.reduce((a, x) => a + (x.type === "log_meal" ? x.protein : 0), 0);
    const remaining = left - kcal;
    const proteinGap = t.protein - proteinIn - prot;
    const tip =
      proteinGap > 30
        ? `باقي عليك حوالي ${n(proteinGap)} غ بروتين؛ خل وجبتك الجاية تبدأ ببروتين (زبادي يوناني، بيض، تونة).`
        : remaining < 0
          ? "طلعت فوق هدف اليوم شوي — عادي جداً. لا تعوّض بالحرمان؛ بكرة نرجع للخطة، والليلة مشي ١٠ دقائق بعد الأكل."
          : `يبقى لك تقريباً ${n(remaining)} سعرة اليوم. 👌`;
    return {
      reply: `حسبتها تقريبياً: ${n(kcal)} سعرة و${n(prot)} غ بروتين. ${tip}`,
      actions,
    };
  }

  if (/(تعبان|تعبانه|مرهق|ما عندي طاقه|كسلان|ما لي خلق|ماني قادر)/.test(q)) {
    return {
      reply: "أفهمك، والتعب مو فشل. اليوم نبي أصغر خطوة تحافظ على الخيط: دقيقتين حركة وأنت جالس، وبعدها أنت حر.",
      actions: [{ type: "start_workout", routineId: "reset-2" }],
    };
  }

  if (/(الليل|بالليل|اخر الليل|سهر|قبل النوم).*(جوع|اكل|جوعان|اشتهي)|(جوع|جوعان|اشتهي).*(الليل|بالليل)/.test(q)) {
    return {
      reply: "جوع الليل غالباً تعب أو ملل أكثر منه جوع حقيقي. جرّب ٥ دقائق تهدئة، وإذا بعدها جوعان: زبادي يوناني أو شاي بدون سكر.",
      actions: [
        { type: "start_workout", routineId: "night-5" },
        { type: "add_if_then", when: "إذا جاني جوع بعد الساعة ٩", then: "أشرب كوب ماء أو شاي، وإذا استمر آكل زبادي يوناني" },
      ],
    };
  }

  if (/(عزيمه|عزومه|وليمه|مناسبه|عرس|ضيوف|مطعم)/.test(q)) {
    return {
      reply: "العزايم جزء من حياتنا، ما نبي نهرب منها. الخطة: صحن واحد، نصه سلطة ومشاوي، ربع رز، وتحلية صغيرة إذا تبي. وقبلها بساعتين وجبة بروتين خفيفة عشان ما توصل جوعان.",
      actions: [{ type: "add_if_then", when: "إذا عندي عزيمة", then: "آكل بروتين خفيف قبلها وآخذ صحن واحد: نص خضار، ربع بروتين، ربع رز" }],
    };
  }

  if (/(ما نزل|زاد وزني|وزني زاد|ثابت|ما تغير|نفس الوزن)/.test(q)) {
    const days = Object.values(s.days).filter((d) => d.weightKg !== null).map((d) => ({ date: d.date, kg: d.weightKg as number }));
    const tr = trendWeights(days);
    const trendLine =
      tr.length >= 2 ? ` خط اتجاهك: ${n(tr[0].trend)} ← ${n(tr[tr.length - 1].trend)} كغ.` : "";
    return {
      reply: `الميزان اليومي يتأثر بالماء والملح والنوم، ممكن يتحرك كيلو بيوم واحد. نحكم على الاتجاه الأسبوعي مو القراءة.${trendLine} استمر أسبوعين بتسجيل صادق، وسند يعدّل هدفك تلقائياً حسب حرقك الحقيقي.`,
      actions: [],
    };
  }

  if (/(حلا|حلويات|سكر|شوكولا|ابي حلو)/.test(q)) {
    return {
      reply: "ما في أكل ممنوع. خذ حصة صغيرة وأنت جالس ومستمتع، بعد وجبة فيها بروتين — مو على جوع. والأهم: سجّلها بدون تأنيب.",
      actions: [],
    };
  }

  const energyNote = today?.energy === 1 ? "بما إن طاقتك اليوم تحت، " : "";
  return {
    reply: `${energyNote}خلنا نركّز على شي واحد: ${proteinIn < t.protein / 2 ? `البروتين (أكلت ${n(proteinIn)} من ${n(t.protein)} غ)` : "حركة قصيرة بعد وجبتك الجاية"}. قل لي وش أكلت اليوم وأحسبه لك، أو قل "تعبان" وأعطيك أخف خطة.`,
    actions: [],
  };
}

export function coachContext(s: AppState, t: Targets): string {
  const p = s.profile!;
  const today = s.days[todayKey()];
  const eaten = today ? dayIntake(today) : 0;
  const prot = today ? today.meals.reduce((a, m) => a + m.protein, 0) : 0;
  const weights = Object.values(s.days)
    .filter((d) => d.weightKg !== null)
    .map((d) => ({ date: d.date, kg: d.weightKg as number }));
  const tr = trendWeights(weights);
  const lastTrend = tr.length ? tr[tr.length - 1].trend : p.startWeightKg;
  return [
    `name: ${p.name}; sex: ${p.sex}; age: ${p.age}; height_cm: ${p.heightCm}`,
    `start_kg: ${p.startWeightKg}; trend_kg: ${lastTrend}; goal_kg: ${p.goalWeightKg}`,
    `targets: ${t.kcal} kcal, ${t.protein} g protein, ${t.steps} steps, ${t.water} cups water`,
    `today: eaten ${eaten} kcal, protein ${prot} g, water ${today?.water ?? 0} cups, energy ${today?.energy ?? "not checked in"} (1 low–3 high), free minutes ${today?.time ?? "?"}`,
    `today_meals: ${today?.meals.map((m) => `${m.name} ${m.kcal}kcal`).join(", ") || "none"}`,
    `why: ${p.why || "-"}; barriers: ${p.barriers.join(",") || "-"}; ramadan_mode: ${p.ramadan}`,
    `health_flags: ${p.flags.join(",") || "none"}`,
    `if_then_plans: ${p.ifThens.map((r) => `${r.when} → ${r.then}`).join(" | ") || "none"}`,
    `local_time: ${new Date().toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" })}`,
  ].join("\n");
}
