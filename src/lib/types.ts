import type { SafetyFlag } from "./safety";

export type Sex = "m" | "f";
export type Activity = "sedentary" | "light" | "moderate" | "active";
export type Pace = "gentle" | "steady" | "brisk";
/** 1 = منخفضة، 2 = متوسطة، 3 = عالية */
export type Energy = 1 | 2 | 3;
/** الدقائق المتاحة للحركة اليوم */
export type TimeBudget = 2 | 10 | 20;

export type Barrier = "time" | "energy" | "night" | "social" | "stress" | "sweets";

export interface IfThen {
  id: string;
  when: string;
  then: string;
}

export interface Profile {
  name: string;
  sex: Sex;
  age: number;
  heightCm: number;
  startWeightKg: number;
  goalWeightKg: number;
  activity: Activity;
  pace: Pace;
  why: string;
  barriers: Barrier[];
  ifThens: IfThen[];
  ramadan: boolean;
  flags: SafetyFlag[];
  createdAt: string;
}

export interface MealEntry {
  id: string;
  name: string;
  kcal: number;
  protein: number;
  at: string;
  source: "db" | "coach" | "quick";
}

export interface WorkoutEntry {
  id: string;
  routineId: string;
  name: string;
  minutes: number;
  at: string;
}

export interface DayLog {
  date: string;
  energy: Energy | null;
  time: TimeBudget | null;
  meals: MealEntry[];
  workouts: WorkoutEntry[];
  water: number;
  steps: number;
  done: string[];
  weightKg: number | null;
}

export interface ChatMessage {
  id: string;
  role: "user" | "coach";
  text: string;
  at: string;
  actions?: CoachAction[];
  applied?: number[];
  offline?: boolean;
}

export type CoachAction =
  | { type: "log_meal"; name: string; kcal: number; protein: number }
  | { type: "log_water"; cups: number }
  | { type: "start_workout"; routineId: string }
  | { type: "add_if_then"; when: string; then: string };

export interface AppState {
  version: 1;
  profile: Profile | null;
  days: Record<string, DayLog>;
  chat: ChatMessage[];
  favorites: string[];
}
