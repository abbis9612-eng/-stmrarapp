import Anthropic from "@anthropic-ai/sdk";
import { z } from "zod";
import { COACH_SCHEMA, COACH_SYSTEM } from "@/lib/coach-prompt";

export const runtime = "nodejs";

const RequestSchema = z.object({
  messages: z
    .array(z.object({ role: z.enum(["user", "coach"]), text: z.string().min(1).max(2000) }))
    .min(1)
    .max(40),
  context: z.string().max(4000),
});

const ActionSchema = z.discriminatedUnion("type", [
  z.object({ type: z.literal("log_meal"), name: z.string().min(1).max(80), kcal: z.number().int().min(0).max(4000), protein: z.number().int().min(0).max(300) }),
  z.object({ type: z.literal("log_water"), cups: z.number().int().min(1).max(10) }),
  z.object({
    type: z.literal("start_workout"),
    routineId: z.enum(["reset-2", "wake-2", "night-5", "low-impact-10", "strength-10", "walk-20", "strength-20"]),
  }),
  z.object({ type: z.literal("add_if_then"), when: z.string().min(1).max(160), then: z.string().min(1).max(160) }),
]);

const ReplySchema = z.object({ reply: z.string().min(1), actions: z.array(ActionSchema).max(8) });

const MODEL = process.env.COACH_MODEL || "claude-opus-5";

// تطبيق أندرويد (Capacitor) يطلب من أصل https://localhost
const APP_ORIGINS = new Set(["https://localhost", "capacitor://localhost", "http://localhost"]);

function cors(req: Request): Record<string, string> {
  const origin = req.headers.get("origin");
  if (!origin || !APP_ORIGINS.has(origin)) return {};
  return {
    "Access-Control-Allow-Origin": origin,
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type",
    Vary: "Origin",
  };
}

export function OPTIONS(req: Request) {
  return new Response(null, { status: 204, headers: cors(req) });
}

export async function POST(req: Request) {
  const res = await handle(req);
  for (const [k, v] of Object.entries(cors(req))) res.headers.set(k, v);
  return res;
}

async function handle(req: Request): Promise<Response> {
  if (!process.env.ANTHROPIC_API_KEY && !process.env.ANTHROPIC_AUTH_TOKEN) {
    return Response.json({ error: "no_key" }, { status: 503 });
  }

  let body: z.infer<typeof RequestSchema>;
  try {
    body = RequestSchema.parse(await req.json());
  } catch {
    return Response.json({ error: "bad_request" }, { status: 400 });
  }

  // آخر ١٦ رسالة تكفي للسياق وتبقي التكلفة معقولة؛ يجب أن تبدأ برسالة مستخدم.
  const recent = body.messages.slice(-16);
  while (recent.length && recent[0].role !== "user") recent.shift();
  if (!recent.length || recent[recent.length - 1].role !== "user") {
    return Response.json({ error: "bad_request" }, { status: 400 });
  }

  const messages: Anthropic.Beta.BetaMessageParam[] = recent.map((m, i) => {
    const isLast = i === recent.length - 1;
    if (m.role === "coach") return { role: "assistant", content: m.text };
    return {
      role: "user",
      content: isLast ? `<app_context>\n${body.context}\n</app_context>\n\n${m.text}` : m.text,
    };
  });

  const client = new Anthropic();
  try {
    const response = await client.beta.messages.create({
      model: MODEL,
      max_tokens: 4000,
      betas: ["server-side-fallback-2026-07-01"],
      fallbacks: "default",
      system: [{ type: "text", text: COACH_SYSTEM, cache_control: { type: "ephemeral" } }],
      output_config: {
        effort: "low",
        format: { type: "json_schema", schema: COACH_SCHEMA as unknown as Record<string, unknown> },
      },
      messages,
    });

    if (response.stop_reason === "refusal") {
      return Response.json({
        reply: "ما أقدر أساعد في هذا الطلب. لو عندك سؤال عن أكلك أو حركتك اليوم، أنا موجود.",
        actions: [],
      });
    }

    const text = response.content.flatMap((b) => (b.type === "text" ? [b.text] : [])).join("");
    const parsed = ReplySchema.safeParse(JSON.parse(text));
    if (!parsed.success) {
      return Response.json({ error: "bad_model_output" }, { status: 502 });
    }
    return Response.json(parsed.data);
  } catch (err) {
    if (err instanceof Anthropic.RateLimitError) return Response.json({ error: "busy" }, { status: 429 });
    if (err instanceof Anthropic.AuthenticationError) return Response.json({ error: "no_key" }, { status: 503 });
    if (err instanceof Anthropic.APIError) return Response.json({ error: "upstream" }, { status: 502 });
    if (err instanceof SyntaxError) return Response.json({ error: "bad_model_output" }, { status: 502 });
    throw err;
  }
}
