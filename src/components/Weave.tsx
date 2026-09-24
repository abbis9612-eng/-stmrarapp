import { addDays, isCounted } from "@/lib/science";
import type { DayLog } from "@/lib/types";

type Cell = "woven" | "held" | "broken" | "today" | "before";

/**
 * الخيط: كل يوم التزمت فيه = صف منسوج بزخرفة السدو.
 * يوم واحد فائت = خيط رفيع يمسك النسيج. يومين = انقطاع.
 * الزمن يمشي من اليمين (الأقدم) لليسار (اليوم) كما نقرأ.
 */
export function Weave({
  days,
  today,
  count = 28,
  animateLast = false,
  firstDay,
}: {
  days: Record<string, DayLog>;
  today: string;
  count?: number;
  animateLast?: boolean;
  firstDay?: string;
}) {
  const dates = Array.from({ length: count }, (_, i) => addDays(today, i - count + 1));
  const counted = dates.map((d) => isCounted(days[d]));
  const cells: Cell[] = dates.map((d, i) => {
    if (firstDay && d < firstDay) return "before";
    if (counted[i]) return "woven";
    if (d === today) return "today";
    const prevMissed = i > 0 && !counted[i - 1] && !(firstDay && dates[i - 1] < firstDay);
    const nextMissed = i < count - 1 && !counted[i + 1] && dates[i + 1] !== today;
    return prevMissed || nextMissed ? "broken" : "held";
  });

  const w = 13;
  const h = 56;
  const width = count * w;
  const x = (i: number) => width - (i + 1) * w; // RTL: الأحدث يساراً
  const palette = ["var(--sadu-2)", "var(--date)", "#f4efe6"];
  const wovenCount = cells.filter((c) => c === "woven").length;

  return (
    <svg className="weave" viewBox={`0 0 ${width} ${h}`} role="img" aria-label={`نسيج آخر ${count} يوم: ${wovenCount} يوم ملتزم`}>
      {cells.map((c, i) => {
        const x0 = x(i);
        const cx = x0 + w / 2;
        const isLast = i === count - 1;
        if (c === "before") {
          return <line key={i} x1={x0} x2={x0 + w} y1={h / 2} y2={h / 2} stroke="rgba(255,255,255,0.12)" strokeDasharray="1 3" />;
        }
        if (c === "broken") {
          return <circle key={i} cx={cx} cy={h / 2} r={1.2} fill="rgba(255,255,255,0.25)" />;
        }
        if (c === "held" || c === "today") {
          return (
            <g key={i}>
              <line x1={x0} x2={x0 + w} y1={h / 2} y2={h / 2} stroke="var(--date)" strokeWidth={1.5} />
              {c === "today" && <rect x={x0 + 1} y={6} width={w - 2} height={h - 12} rx={3} fill="none" stroke="rgba(255,255,255,0.55)" strokeDasharray="2 2" />}
            </g>
          );
        }
        const col = palette[i % 3];
        return (
          <g key={i} className={animateLast && isLast ? "row-new" : undefined}>
            <rect x={x0} y={4} width={w} height={6} fill="var(--sadu)" />
            <rect x={x0} y={h - 10} width={w} height={6} fill="var(--sadu)" />
            <rect x={x0} y={12} width={w} height={2} fill="#f4efe6" opacity={0.85} />
            <rect x={x0} y={h - 14} width={w} height={2} fill="#f4efe6" opacity={0.85} />
            <path d={`M${cx} 17 L${x0 + w - 1} ${h / 2} L${cx} ${h - 17} L${x0 + 1} ${h / 2} Z`} fill={col} />
            <path d={`M${cx} 23 L${cx + 2.5} ${h / 2} L${cx} ${h - 23} L${cx - 2.5} ${h / 2} Z`} fill="var(--night)" />
          </g>
        );
      })}
    </svg>
  );
}
