import Anthropic from "@anthropic-ai/sdk";
import { COACH_SCHEMA, COACH_SYSTEM } from "@/lib/coach-prompt.generated";
import { CoachReply, CoachRequest, INSTALL_RE, recentTurns, safeEqual } from "@/lib/server/coach-api";
import { checkAndCount, limitsFromEnv, MemoryCounter, UpstashCounter, type Counter } from "@/lib/server/limits";

/**
 * المدرب السحابي لتطبيق أندرويد: المفتاح يبقى هنا بالسيرفر، والتطبيق يرسل
 * آخر الرسائل + سياق اليوم (+ صورة وجبة اختيارية). محمي بمفتاح التطبيق
 * وحد يومي لكل جهاز، وحد الإنفاق الشهري يُضبط بلوحة Anthropic.
 */
export const runtime = "nodejs";
export const maxDuration = 60;

const MODEL = process.env.COACH_MODEL || "claude-opus-5";
// Haiku 4.5 ما يقبل effort، والـfallbacks الافتراضية مخصصة لعائلة Opus 5 / Fable
const SUPPORTS_EFFORT = !MODEL.startsWith("claude-haiku");
const USE_FALLBACKS = /^claude-(opus-5|fable-5)/.test(MODEL);
const LIMITS = limitsFromEnv(process.env);

let counter: Counter | undefined;
function getCounter(): Counter {
  if (!counter) {
    const url = process.env.UPSTASH_REDIS_REST_URL;
    const token = process.env.UPSTASH_REDIS_REST_TOKEN;
    counter = url && token ? new UpstashCounter(url, token) : new MemoryCounter();
  }
  return counter;
}

const REFUSAL_REPLY = {
  reply: "ما أقدر أساعد في هذا الطلب. لو عندك سؤال عن أكلك أو حركتك اليوم، أنا موجود.",
  actions: [],
};

export async function POST(req: Request): Promise<Response> {
  if (!process.env.ANTHROPIC_API_KEY && !process.env.ANTHROPIC_AUTH_TOKEN) {
    return Response.json({ error: "no_key" }, { status: 503 });
  }

  const appKey = process.env.SANAD_APP_KEY;
  if (appKey && !safeEqual(req.headers.get("x-sanad-key") ?? "", appKey)) {
    return Response.json({ error: "unauthorized" }, { status: 401 });
  }
  const install = req.headers.get("x-sanad-install") ?? "";
  if (!INSTALL_RE.test(install)) return Response.json({ error: "unauthorized" }, { status: 401 });

  let body: CoachRequest;
  try {
    body = CoachRequest.parse(await req.json());
  } catch {
    return Response.json({ error: "bad_request" }, { status: 400 });
  }
  const recent = recentTurns(body.messages);
  if (!recent) return Response.json({ error: "bad_request" }, { status: 400 });

  let verdict;
  try {
    verdict = await checkAndCount(getCounter(), LIMITS, install.toLowerCase(), !!body.image);
  } catch {
    // عطل بعدّاد Upstash: نكمل بدل ما نوقف المدرب، وحد Anthropic الشهري يبقى الحماية الأخيرة
    verdict = { ok: true as const, remaining: { messages: -1, photos: -1 } };
  }
  if (!verdict.ok) return Response.json({ error: "limit", reason: verdict.reason }, { status: 429 });

  const messages: Anthropic.Beta.BetaMessageParam[] = recent.map((m, i) => {
    if (m.role === "coach") return { role: "assistant", content: m.text };
    if (i !== recent.length - 1) return { role: "user", content: m.text };
    const text = `<app_context>\n${body.context}\n</app_context>\n\n${m.text}`;
    if (!body.image) return { role: "user", content: text };
    return {
      role: "user",
      content: [
        { type: "image", source: { type: "base64", media_type: body.image.mime, data: body.image.data } },
        { type: "text", text },
      ],
    };
  });

  const client = new Anthropic();
  try {
    const response = await client.beta.messages.create({
      model: MODEL,
      max_tokens: 4000,
      ...(USE_FALLBACKS ? { betas: ["server-side-fallback-2026-07-01"], fallbacks: "default" as const } : {}),
      system: [{ type: "text", text: COACH_SYSTEM, cache_control: { type: "ephemeral" } }],
      output_config: {
        ...(SUPPORTS_EFFORT ? { effort: "low" as const } : {}),
        format: { type: "json_schema", schema: COACH_SCHEMA as unknown as Record<string, unknown> },
      },
      messages,
    });

    if (response.stop_reason === "refusal") return Response.json({ ...REFUSAL_REPLY, remaining: verdict.remaining });

    const text = response.content.flatMap((b) => (b.type === "text" ? [b.text] : [])).join("");
    const parsed = CoachReply.safeParse(JSON.parse(text));
    if (!parsed.success) return Response.json({ error: "bad_model_output" }, { status: 502 });
    return Response.json({ ...parsed.data, remaining: verdict.remaining });
  } catch (err) {
    if (err instanceof Anthropic.RateLimitError) return Response.json({ error: "busy" }, { status: 429 });
    if (err instanceof Anthropic.AuthenticationError) return Response.json({ error: "no_key" }, { status: 503 });
    if (err instanceof Anthropic.APIError) return Response.json({ error: "upstream" }, { status: 502 });
    if (err instanceof SyntaxError) return Response.json({ error: "bad_model_output" }, { status: 502 });
    throw err;
  }
}
