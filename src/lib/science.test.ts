import { describe, expect, it } from "vitest";
import {
  adaptiveTdee,
  addDays,
  bmr,
  computeTargets,
  computeThread,
  dayMissions,
  proteinTarget,
  referenceWeight,
  trendWeights,
} from "./science";
import type { DayLog, Profile } from "./types";
import { parseMealText, searchFoods } from "./foods";
import { assessSafety } from "./safety";
import { ROUTINES, routineSeconds } from "./workouts";

const profile: Profile = {
  name: "سارة",
  sex: "f",
  age: 34,
  heightCm: 162,
  startWeightKg: 88,
  goalWeightKg: 70,
  activity: "sedentary",
  pace: "steady",
  why: "",
  barriers: ["time", "night"],
  ifThens: [],
  ramadan: false,
  flags: [],
  createdAt: "2026-09-01",
};

const day = (date: string, over: Partial<DayLog> = {}): DayLog => ({
  date,
  energy: null,
  time: null,
  meals: [],
  workouts: [],
  water: 0,
  steps: 0,
  done: [],
  weightKg: null,
  ...over,
});

describe("energy math", () => {
  it("Mifflin-St Jeor matches reference values", () => {
    // 10*88 + 6.25*162 - 5*34 - 161 = 1561.5
    expect(bmr("f", 88, 162, 34)).toBeCloseTo(1561.5, 1);
    expect(bmr("m", 100, 180, 40)).toBeCloseTo(1930, 1);
  });

  it("protein uses adjusted weight when overweight and stays in the 1.2–1.6 g/kg band of it", () => {
    const ref = referenceWeight(88, 162);
    expect(ref).toBeLessThan(88);
    const p = proteinTarget(88, 162);
    expect(p).toBeGreaterThanOrEqual(1.2 * ref - 5);
    expect(p).toBeLessThanOrEqual(1.6 * ref + 5);
  });

  it("targets never go under the sex-specific floor", () => {
    const tiny: Profile = { ...profile, heightCm: 150, age: 60, pace: "brisk", startWeightKg: 62 };
    const t = computeTargets(tiny, 62);
    expect(t.kcal).toBeGreaterThanOrEqual(1200);
    expect(t.floorApplied).toBe(true);
  });

  it("weekly loss rate is capped under 1% of body weight", () => {
    const t = computeTargets({ ...profile, pace: "brisk" }, 88);
    expect(t.weeklyLossKg / 88).toBeLessThan(0.01);
  });
});

describe("trend weight", () => {
  it("smooths a water-weight spike", () => {
    const pts = [
      { date: "2026-09-01", kg: 88 },
      { date: "2026-09-02", kg: 87.8 },
      { date: "2026-09-03", kg: 89.5 },
    ];
    const t = trendWeights(pts);
    expect(t[2].trend).toBeLessThan(88.3);
  });

  it("gives more weight to a reading after a long gap", () => {
    const short = trendWeights([
      { date: "2026-09-01", kg: 90 },
      { date: "2026-09-02", kg: 85 },
    ]);
    const long = trendWeights([
      { date: "2026-09-01", kg: 90 },
      { date: "2026-09-15", kg: 85 },
    ]);
    expect(long[1].trend).toBeLessThan(short[1].trend);
  });
});

describe("adaptive expenditure", () => {
  it("learns a lower TDEE when weight stays flat on the target intake", () => {
    const days: DayLog[] = [];
    for (let i = 0; i < 21; i++) {
      const date = addDays("2026-09-01", i);
      days.push(
        day(date, {
          meals: [{ id: String(i), name: "x", kcal: 1700, protein: 90, at: date, source: "quick" }],
          weightKg: 88 + (i % 2 ? 0.2 : -0.2),
        }),
      );
    }
    const r = adaptiveTdee(profile, days, "2026-09-21");
    expect(r).not.toBeNull();
    // الوزن ثابت على 1700 ⇒ الحرق الحقيقي أقرب لـ 1700 من تقدير المعادلة (~1870)
    expect(r!.tdee).toBeLessThan(1860);
    expect(r!.confidence).toBeGreaterThan(0.5);
  });

  it("returns null without enough data", () => {
    expect(adaptiveTdee(profile, [day("2026-09-01", { weightKg: 88 })], "2026-09-02")).toBeNull();
  });
});

describe("the thread (never miss twice)", () => {
  const counted = (date: string) => day(date, { done: ["move"] });

  it("survives a single missed day", () => {
    const days = {
      "2026-09-01": counted("2026-09-01"),
      "2026-09-02": counted("2026-09-02"),
      "2026-09-04": counted("2026-09-04"),
    };
    expect(computeThread(days, "2026-09-04").length).toBe(3);
  });

  it("breaks after two missed days in a row", () => {
    const days = {
      "2026-09-01": counted("2026-09-01"),
      "2026-09-02": counted("2026-09-02"),
      "2026-09-05": counted("2026-09-05"),
    };
    const t = computeThread(days, "2026-09-05");
    expect(t.length).toBe(1);
    expect(t.best).toBe(2);
  });

  it("flags today as a rescue day after one miss", () => {
    const days = { "2026-09-01": counted("2026-09-01") };
    const t = computeThread(days, "2026-09-03");
    expect(t.rescueToday).toBe(true);
    expect(t.length).toBe(1);
  });
});

describe("day missions adapt to energy and time", () => {
  const t = computeTargets(profile, 88);
  it("low energy shrinks movement to two minutes", () => {
    const m = dayMissions(1, 20, t, profile);
    expect(m[0].routineId).toBe("reset-2");
  });
  it("high energy with time gets full strength session", () => {
    const m = dayMissions(3, 20, t, profile);
    expect(m[0].routineId).toBe("strength-20");
  });
  it("night-eating barrier gets a kitchen-closing mission", () => {
    const m = dayMissions(2, 10, t, profile);
    expect(m[2].title).toContain("المطبخ");
  });
  it("every referenced routine exists", () => {
    for (const e of [1, 2, 3] as const)
      for (const time of [2, 10, 20] as const)
        for (const mission of dayMissions(e, time, t, profile))
          if (mission.routineId) expect(ROUTINES.some((r) => r.id === mission.routineId)).toBe(true);
  });
});

describe("food search and parsing", () => {
  it("finds dishes ignoring hamza and taa marbuta", () => {
    expect(searchFoods("كبسه")[0].id).toBe("kabsa-chicken");
    expect(searchFoods("شاورما")[0].id).toMatch(/shawarma/);
  });

  it("parses a free Gulf-dialect sentence into items", () => {
    const items = parseMealText("تغديت كبسة دجاج ولبن ونص صحن سلطة");
    const ids = items.map((i) => i.food.id);
    expect(ids).toContain("kabsa-chicken");
    expect(ids).toContain("laban");
  });

  it("understands two eggs", () => {
    const items = parseMealText("فطرت بيضتين وشاي");
    expect(items.map((i) => i.food.id)).toContain("eggs-2");
  });
});

describe("safety", () => {
  it("blocks pregnancy and minors", () => {
    expect(assessSafety({ age: 16, weightKg: 80, heightCm: 165, flags: [] }).block).toBe(true);
    expect(assessSafety({ age: 30, weightKg: 80, heightCm: 165, flags: ["pregnant"] }).block).toBe(true);
  });
  it("adds a note but does not block GLP-1 users", () => {
    const r = assessSafety({ age: 40, weightKg: 100, heightCm: 170, flags: ["glp1"] });
    expect(r.block).toBe(false);
    expect(r.notes.length).toBe(1);
  });
});

describe("routines", () => {
  it("durations roughly match their label", () => {
    for (const r of ROUTINES) {
      const mins = routineSeconds(r) / 60;
      expect(Math.abs(mins - r.minutes)).toBeLessThanOrEqual(Math.max(1, r.minutes * 0.25));
    }
  });
});
