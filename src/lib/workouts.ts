/**
 * "وجبات حركة" قصيرة بدون أدوات. الأبحاث (مراجعات 2025 عن Exercise Snacks)
 * تُظهر أن الدفعات القصيرة تحسّن اللياقة وتحافظ على الكتلة العضلية،
 * والأهم: يسهل الاستمرار عليها لمن ليس لديه وقت أو طاقة.
 */
export interface Move {
  name: string;
  cue: string;
  seconds: number;
  rest?: boolean;
}

export interface Routine {
  id: string;
  title: string;
  minutes: number;
  energy: 1 | 2 | 3;
  tag: string;
  why: string;
  moves: Move[];
}

const rest = (seconds: number): Move => ({ name: "راحة", cue: "تنفّس بعمق من الأنف.", seconds, rest: true });

export const ROUTINES: Routine[] = [
  {
    id: "reset-2",
    title: "إعادة شحن",
    minutes: 2,
    energy: 1,
    tag: "وأنت جالس",
    why: "لأيام التعب: تحريك الدورة الدموية بدون إجهاد. يحسب لك يوم كامل في الخيط.",
    moves: [
      { name: "دوران الكتفين", cue: "ارفع كتفيك للأذن وارجعها للخلف ببطء.", seconds: 30 },
      { name: "رفع الركبة جالساً", cue: "بالتناوب، ظهرك مستقيم.", seconds: 40 },
      { name: "وقوف وجلوس من الكرسي", cue: "ببطء، بدون ما تستند على يديك إن قدرت.", seconds: 50 },
    ],
  },
  {
    id: "wake-2",
    title: "صحصحة دقيقتين",
    minutes: 2,
    energy: 2,
    tag: "بين الاجتماعات",
    why: "دفعة قصيرة ترفع النبض وتكسر الجلوس الطويل.",
    moves: [
      { name: "سكوات للكرسي", cue: "الورك للخلف كأنك بتجلس، ثم قم.", seconds: 40 },
      { name: "ضغط على الجدار", cue: "جسمك خط مستقيم، انزل بصدرك للجدار.", seconds: 40 },
      { name: "مشي سريع بمكانك", cue: "ارفع ركبك وحرّك ذراعيك.", seconds: 40 },
    ],
  },
  {
    id: "low-impact-10",
    title: "١٠ دقائق بدون قفز",
    minutes: 10,
    energy: 2,
    tag: "مناسب للركب",
    why: "حرق وقوة بدون ضغط على المفاصل — مثالي مع الوزن الزائد.",
    moves: [
      { name: "مشي بمكانك", cue: "إحماء: تنفّس براحة.", seconds: 60 },
      { name: "سكوات للكرسي", cue: "انزل ٣ ثوانٍ واطلع بقوة.", seconds: 45 },
      rest(15),
      { name: "ضغط على الطاولة", cue: "يداك على حافة طاولة ثابتة.", seconds: 45 },
      rest(15),
      { name: "خطوة جانبية مع لمس", cue: "يمين ويسار بإيقاع ثابت.", seconds: 60 },
      { name: "جسر الورك", cue: "على ظهرك، ارفع الورك واعصر المؤخرة.", seconds: 45 },
      rest(15),
      { name: "لكمات هوائية", cue: "بطنك مشدود، بدّل اليدين بسرعة.", seconds: 45 },
      { name: "سكوات للكرسي", cue: "الجولة الثانية — أبطأ وأدق.", seconds: 45 },
      rest(15),
      { name: "ضغط على الطاولة", cue: "انزل ببطء.", seconds: 45 },
      rest(15),
      { name: "جسر الورك", cue: "ثبّت فوق ثانيتين.", seconds: 45 },
      { name: "مشي بمكانك", cue: "تهدئة.", seconds: 60 },
      { name: "إطالة الفخذ الخلفي", cue: "واقف، مِل للأمام بظهر مستقيم.", seconds: 45 },
    ],
  },
  {
    id: "strength-10",
    title: "قوة ١٠ دقائق",
    minutes: 10,
    energy: 3,
    tag: "يحمي العضل",
    why: "تمارين المقاومة أثناء النزول تحافظ على العضل وتمنع هبوط الحرق.",
    moves: [
      { name: "سكوات", cue: "القدمين بعرض الكتفين، صدرك مرفوع.", seconds: 45 },
      rest(15),
      { name: "ضغط (على الركب إن احتجت)", cue: "انزل حتى صدرك قريب من الأرض.", seconds: 40 },
      rest(20),
      { name: "طعنات خلفية", cue: "خطوة للخلف، الركبة الخلفية قرب الأرض.", seconds: 45 },
      rest(15),
      { name: "بلانك", cue: "خط مستقيم من الرأس للكعب.", seconds: 30 },
      rest(20),
      { name: "سكوات", cue: "الجولة الثانية.", seconds: 45 },
      rest(15),
      { name: "ضغط", cue: "ركّز على النزول البطيء.", seconds: 40 },
      rest(20),
      { name: "طعنات خلفية", cue: "بدّل الرجلين.", seconds: 45 },
      rest(15),
      { name: "جسر الورك", cue: "ثبّت فوق.", seconds: 45 },
      { name: "بلانك", cue: "آخر تحدي!", seconds: 30 },
      { name: "إطالة", cue: "تنفّس وارخِ كتفيك.", seconds: 60 },
    ],
  },
  {
    id: "strength-20",
    title: "قوة ومشي ٢٠",
    minutes: 20,
    energy: 3,
    tag: "الجلسة الكاملة",
    why: "مقاومة + كارديو معتدل: أفضل تركيبة لحرق الدهون مع حماية العضل.",
    moves: [
      { name: "مشي سريع بمكانك", cue: "إحماء.", seconds: 120 },
      ...Array.from({ length: 3 }).flatMap((_, i) => [
        { name: "سكوات", cue: `الجولة ${["الأولى", "الثانية", "الثالثة"][i]}.`, seconds: 45 },
        rest(15),
        { name: "ضغط", cue: "على الركب أو كامل.", seconds: 40 },
        rest(20),
        { name: "طعنات خلفية", cue: "بالتناوب.", seconds: 45 },
        rest(15),
        { name: "بلانك", cue: "بطنك مشدود.", seconds: 30 },
        rest(30),
      ]),
      { name: "مشي سريع بمكانك", cue: "حافظ على إيقاع تقدر تتكلم فيه.", seconds: 300 },
      { name: "إطالة الجسم كامل", cue: "تهدئة.", seconds: 60 },
    ],
  },
  {
    id: "walk-20",
    title: "مشي ٢٠ دقيقة",
    minutes: 20,
    energy: 2,
    tag: "بعد الأكل أفضل",
    why: "المشي بعد الوجبة يخفّض ارتفاع السكر ويرفع حرقك اليومي بدون إرهاق.",
    moves: [
      { name: "مشي هادئ", cue: "إحماء.", seconds: 180 },
      { name: "مشي بإيقاع أسرع", cue: "تقدر تتكلم لكن ما تقدر تغني.", seconds: 840 },
      { name: "مشي هادئ", cue: "تهدئة.", seconds: 180 },
    ],
  },
  {
    id: "night-5",
    title: "قبل النوم ٥",
    minutes: 5,
    energy: 1,
    tag: "يهدّي الجوع",
    why: "حركة لطيفة وتنفّس تقلل رغبة الأكل الليلي وتحسّن النوم.",
    moves: [
      { name: "تنفّس ٤-٦", cue: "شهيق ٤ ثوانٍ، زفير ٦.", seconds: 60 },
      { name: "إطالة القط والبقرة", cue: "على اليدين والركب، قوّس ظهرك ببطء.", seconds: 60 },
      { name: "وضعية الطفل", cue: "ارخِ جسمك للخلف.", seconds: 60 },
      { name: "جسر الورك البطيء", cue: "ارفع وانزل مع النفس.", seconds: 60 },
      { name: "تنفّس ٤-٦", cue: "وخلاص — المطبخ مسكّر 🙂", seconds: 60 },
    ],
  },
];

export const routineById = (id: string) => ROUTINES.find((r) => r.id === id);

export const routineSeconds = (r: Routine) => r.moves.reduce((s, m) => s + m.seconds, 0);
