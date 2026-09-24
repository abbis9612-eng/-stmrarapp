"use client";

import { useSyncExternalStore } from "react";
import type { AppState, ChatMessage, CoachAction, DayLog, Energy, IfThen, MealEntry, Profile, TimeBudget } from "./types";
import { computeTargets, adaptiveTdee, type Targets } from "./science";
import { routineById } from "./workouts";

const KEY = "sanad:v1";

export const emptyState = (): AppState => ({ version: 1, profile: null, days: {}, chat: [], favorites: [] });

export function todayKey(d = new Date()): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

export const uid = () => Math.random().toString(36).slice(2, 10) + Date.now().toString(36).slice(-4);

export const emptyDay = (date: string): DayLog => ({
  date,
  energy: null,
  time: null,
  meals: [],
  workouts: [],
  water: 0,
  steps: 0,
  done: [],
  weightKg: null,
});

let state: AppState = emptyState();
let hydrated = false;
const listeners = new Set<() => void>();

function load(): AppState {
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) return emptyState();
    const parsed = JSON.parse(raw) as AppState;
    if (parsed.version !== 1) return emptyState();
    return { ...emptyState(), ...parsed };
  } catch {
    return emptyState();
  }
}

function persist() {
  try {
    localStorage.setItem(KEY, JSON.stringify(state));
  } catch {
    /* التخزين ممتلئ أو محجوب — نكمل في الذاكرة */
  }
}

function ensureHydrated() {
  if (!hydrated && typeof window !== "undefined") {
    state = load();
    hydrated = true;
  }
}

function set(next: AppState) {
  state = next;
  persist();
  listeners.forEach((l) => l());
}

function subscribe(l: () => void) {
  ensureHydrated();
  listeners.add(l);
  return () => listeners.delete(l);
}

const serverSnapshot = emptyState();

export function useApp(): AppState & { ready: boolean } {
  const s = useSyncExternalStore(
    subscribe,
    () => {
      ensureHydrated();
      return state;
    },
    () => serverSnapshot,
  );
  const ready = useSyncExternalStore(noopSubscribe, () => true, () => false);
  return { ...s, ready };
}

const noopSubscribe = () => () => {};

export const getState = () => {
  ensureHydrated();
  return state;
};

function updateDay(date: string, fn: (d: DayLog) => DayLog) {
  const s = getState();
  const current = s.days[date] ?? emptyDay(date);
  set({ ...s, days: { ...s.days, [date]: fn(current) } });
}

/* ----------------------------- الإجراءات ----------------------------- */

export const actions = {
  saveProfile(profile: Profile) {
    const s = getState();
    set({ ...s, profile });
    const today = todayKey();
    updateDay(today, (d) => ({ ...d, weightKg: d.weightKg ?? profile.startWeightKg }));
  },

  checkIn(energy: Energy, time: TimeBudget, date = todayKey()) {
    updateDay(date, (d) => ({ ...d, energy, time }));
  },

  toggleMission(id: string, date = todayKey()) {
    updateDay(date, (d) => ({ ...d, done: d.done.includes(id) ? d.done.filter((x) => x !== id) : [...d.done, id] }));
  },

  addMeal(meal: Omit<MealEntry, "id" | "at">, date = todayKey()) {
    updateDay(date, (d) => {
      const meals = [...d.meals, { ...meal, id: uid(), at: new Date().toISOString() }];
      return { ...d, meals, done: d.done.includes("eat") ? d.done : [...d.done, "eat"] };
    });
  },

  removeMeal(id: string, date = todayKey()) {
    updateDay(date, (d) => ({ ...d, meals: d.meals.filter((m) => m.id !== id) }));
  },

  addWater(delta: number, date = todayKey()) {
    updateDay(date, (d) => ({ ...d, water: Math.max(0, Math.min(20, d.water + delta)) }));
  },

  logWorkout(routineId: string, date = todayKey()) {
    const r = routineById(routineId);
    if (!r) return;
    updateDay(date, (d) => ({
      ...d,
      workouts: [...d.workouts, { id: uid(), routineId, name: r.title, minutes: r.minutes, at: new Date().toISOString() }],
      done: d.done.includes("move") ? d.done : [...d.done, "move"],
    }));
  },

  logWeight(kg: number, date = todayKey()) {
    updateDay(date, (d) => ({ ...d, weightKg: Math.round(kg * 10) / 10 }));
  },

  toggleFavorite(foodId: string) {
    const s = getState();
    const favorites = s.favorites.includes(foodId) ? s.favorites.filter((f) => f !== foodId) : [...s.favorites, foodId];
    set({ ...s, favorites });
  },

  addIfThen(rule: Omit<IfThen, "id">) {
    const s = getState();
    if (!s.profile) return;
    set({ ...s, profile: { ...s.profile, ifThens: [...s.profile.ifThens, { ...rule, id: uid() }] } });
  },

  removeIfThen(id: string) {
    const s = getState();
    if (!s.profile) return;
    set({ ...s, profile: { ...s.profile, ifThens: s.profile.ifThens.filter((r) => r.id !== id) } });
  },

  pushChat(msg: Omit<ChatMessage, "id" | "at">) {
    const s = getState();
    const chat = [...s.chat, { ...msg, id: uid(), at: new Date().toISOString() }].slice(-80);
    set({ ...s, chat });
  },

  /** ينفّذ اقتراح المدرب بعد موافقة المستخدم، ويرجع وصفاً قصيراً لما تم. */
  applyCoachAction(a: CoachAction): string {
    switch (a.type) {
      case "log_meal":
        actions.addMeal({ name: a.name, kcal: Math.round(a.kcal), protein: Math.round(a.protein), source: "coach" });
        return `سُجّل: ${a.name}`;
      case "log_water":
        actions.addWater(a.cups);
        return `+${a.cups} ماء`;
      case "add_if_then":
        actions.addIfThen({ when: a.when, then: a.then });
        return "أُضيفت الخطة";
      case "start_workout":
        return "";
    }
  },

  markActionApplied(msgId: string, index: number) {
    const s = getState();
    const chat = s.chat.map((m) => (m.id === msgId ? { ...m, applied: [...(m.applied ?? []), index] } : m));
    set({ ...s, chat });
  },

  clearChat() {
    const s = getState();
    set({ ...s, chat: [] });
  },

  reset() {
    set(emptyState());
  },

  importState(json: string): boolean {
    try {
      const parsed = JSON.parse(json) as AppState;
      if (parsed.version !== 1) return false;
      set({ ...emptyState(), ...parsed });
      return true;
    } catch {
      return false;
    }
  },
};

/* ----------------------------- مشتقات ----------------------------- */

export function latestWeight(s: AppState): number | null {
  const keys = Object.keys(s.days).sort().reverse();
  for (const k of keys) {
    const w = s.days[k].weightKg;
    if (w !== null) return w;
  }
  return s.profile?.startWeightKg ?? null;
}

export function deriveTargets(s: AppState): (Targets & { adaptive: ReturnType<typeof adaptiveTdee> }) | null {
  if (!s.profile) return null;
  const w = latestWeight(s) ?? s.profile.startWeightKg;
  const adaptive = adaptiveTdee(s.profile, Object.values(s.days), todayKey());
  return { ...computeTargets(s.profile, w, adaptive?.tdee), adaptive };
}
