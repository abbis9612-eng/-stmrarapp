import { z } from "zod";

/** عقد الطلب بين تطبيق أندرويد وسيرفر المدرب (الإصدار ١). */
export const INSTALL_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

// ~٢ ميغابايت صورة بعد base64؛ التطبيق يصغّرها لـ~١٢٨٠px JPEG قبل الإرسال
export const MAX_IMAGE_B64 = 2_800_000;

export const CoachRequest = z.object({
  messages: z
    .array(z.object({ role: z.enum(["user", "coach"]), text: z.string().min(1).max(2000) }))
    .min(1)
    .max(40),
  context: z.string().max(6000),
  image: z
    .object({
      mime: z.enum(["image/jpeg", "image/png", "image/webp"]),
      data: z.string().min(100).max(MAX_IMAGE_B64).regex(/^[A-Za-z0-9+/]+={0,2}$/),
    })
    .optional(),
});
export type CoachRequest = z.infer<typeof CoachRequest>;

export const CoachAction = z.discriminatedUnion("type", [
  z.object({ type: z.literal("log_meal"), name: z.string().min(1).max(80), kcal: z.number().int().min(0).max(4000), protein: z.number().int().min(0).max(300) }),
  z.object({ type: z.literal("log_water"), cups: z.number().int().min(1).max(10) }),
  z.object({
    type: z.literal("start_workout"),
    routineId: z.enum(["reset-2", "wake-2", "night-5", "low-impact-10", "strength-10", "walk-20", "strength-20"]),
  }),
  z.object({ type: z.literal("add_if_then"), when: z.string().min(1).max(160), then: z.string().min(1).max(160) }),
]);

export const CoachReply = z.object({ reply: z.string().min(1).max(4000), actions: z.array(CoachAction).max(8) });

/** آخر ١٦ رسالة تبدأ برسالة مستخدم؛ null إذا آخر رسالة مو من المستخدم. */
export function recentTurns(messages: CoachRequest["messages"]): CoachRequest["messages"] | null {
  const recent = messages.slice(-16);
  while (recent.length && recent[0].role !== "user") recent.shift();
  if (!recent.length || recent[recent.length - 1].role !== "user") return null;
  return recent;
}

/** مقارنة بزمن ثابت لمفتاح التطبيق. */
export function safeEqual(a: string, b: string): boolean {
  if (a.length !== b.length) return false;
  let d = 0;
  for (let i = 0; i < a.length; i++) d |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return d === 0;
}
