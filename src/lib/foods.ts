/**
 * قاعدة أكلات خليجية وعربية شائعة بحصص واقعية.
 * القيم تقديرية (متوسطات من جداول تغذية منشورة ووصفات منزلية) —
 * الهدف تسجيل سريع وصادق، لا دقة مخبرية.
 */
export interface Food {
  id: string;
  name: string;
  portion: string;
  kcal: number;
  protein: number;
  cat: "main" | "bread" | "protein" | "snack" | "sweet" | "drink" | "fruit" | "breakfast";
  aliases?: string[];
  /** حصة بروتين عالية مقارنة بالسعرات */
  proteinStar?: boolean;
}

export const FOODS: Food[] = [
  // أطباق رئيسية
  { id: "kabsa-chicken", name: "كبسة دجاج", portion: "صحن متوسط (رز + ربع دجاجة)", kcal: 650, protein: 38, cat: "main", aliases: ["كبسه", "رز ودجاج", "مكبوس", "مجبوس دجاج", "مجبوس"] },
  { id: "kabsa-meat", name: "كبسة لحم", portion: "صحن متوسط", kcal: 760, protein: 36, cat: "main", aliases: ["رز ولحم", "مجبوس لحم"] },
  { id: "mandi-chicken", name: "مندي دجاج", portion: "صحن متوسط", kcal: 700, protein: 40, cat: "main", aliases: ["مندي"] },
  { id: "madhbi", name: "مضبي / مشوي على الحجر", portion: "ربع دجاجة + رز قليل", kcal: 520, protein: 42, cat: "main", proteinStar: true },
  { id: "rice-plain", name: "رز أبيض", portion: "كوب مطبوخ", kcal: 205, protein: 4, cat: "main", aliases: ["رز", "عيش"] },
  { id: "harees", name: "هريس", portion: "صحن متوسط", kcal: 420, protein: 24, cat: "main", aliases: ["هريسه"] },
  { id: "jareesh", name: "جريش", portion: "صحن متوسط", kcal: 380, protein: 14, cat: "main" },
  { id: "saleeg", name: "سليق", portion: "صحن متوسط", kcal: 560, protein: 28, cat: "main" },
  { id: "margoog", name: "مرقوق / مطازيز", portion: "صحن متوسط", kcal: 480, protein: 22, cat: "main", aliases: ["مطازيز", "مرقوق"] },
  { id: "thareed", name: "ثريد", portion: "صحن متوسط", kcal: 520, protein: 26, cat: "main" },
  { id: "machboos-fish", name: "مجبوس سمك", portion: "صحن متوسط", kcal: 600, protein: 38, cat: "main", aliases: ["رز وسمك", "صيادية"] },
  { id: "grilled-fish", name: "سمك مشوي", portion: "قطعة ٢٠٠ غ", kcal: 280, protein: 44, cat: "protein", aliases: ["سمك", "هامور", "كنعد", "صافي"], proteinStar: true },
  { id: "shawarma-chicken", name: "شاورما دجاج", portion: "ساندويتش عادي", kcal: 480, protein: 26, cat: "main", aliases: ["شاورما", "شورما"] },
  { id: "shawarma-plate", name: "صحن شاورما عربي", portion: "صحن مع بطاطس وثوم", kcal: 1050, protein: 45, cat: "main", aliases: ["عربي شاورما"] },
  { id: "falafel-sandwich", name: "ساندويتش فلافل", portion: "ساندويتش", kcal: 420, protein: 13, cat: "main", aliases: ["فلافل", "طعمية"] },
  { id: "foul", name: "فول مدمس", portion: "صحن صغير بزيت قليل", kcal: 300, protein: 16, cat: "breakfast", aliases: ["فول"] },
  { id: "hummus", name: "حمص بطحينة", portion: "صحن صغير", kcal: 250, protein: 8, cat: "snack", aliases: ["حمص"] },
  { id: "mutabbal", name: "متبل", portion: "صحن صغير", kcal: 180, protein: 4, cat: "snack" },
  { id: "tabbouleh", name: "تبولة", portion: "صحن", kcal: 170, protein: 3, cat: "snack", aliases: ["تبوله"] },
  { id: "fattoush", name: "فتوش", portion: "صحن", kcal: 220, protein: 4, cat: "snack", aliases: ["فتوش", "سلطة"] },
  { id: "salad-green", name: "سلطة خضراء بدون صوص", portion: "صحن كبير", kcal: 60, protein: 2, cat: "snack", aliases: ["سلطه خضراء", "خضار"] },
  { id: "lentil-soup", name: "شوربة عدس", portion: "زبدية", kcal: 230, protein: 12, cat: "main", aliases: ["شوربه", "عدس"] },
  { id: "oats-soup", name: "شوربة شوفان", portion: "زبدية", kcal: 180, protein: 7, cat: "main" },
  { id: "mixed-grill", name: "مشاوي مشكلة", portion: "٣ أسياخ بدون خبز", kcal: 560, protein: 55, cat: "protein", aliases: ["مشاوي", "كباب", "شيش طاووق", "تكة"], proteinStar: true },
  { id: "shish-tawook", name: "شيش طاووق", portion: "سيخين", kcal: 330, protein: 42, cat: "protein", aliases: ["طاووق"], proteinStar: true },
  { id: "chicken-breast", name: "صدر دجاج مشوي", portion: "١٥٠ غ", kcal: 250, protein: 46, cat: "protein", aliases: ["صدر دجاج", "دجاج مشوي"], proteinStar: true },
  { id: "broasted", name: "بروستد", portion: "٣ قطع + بطاطس", kcal: 1150, protein: 55, cat: "main", aliases: ["دجاج مقلي", "بروست"] },
  { id: "burger", name: "برجر لحم", portion: "ساندويتش متوسط", kcal: 550, protein: 28, cat: "main", aliases: ["برقر", "همبرجر"] },
  { id: "pizza", name: "بيتزا", portion: "شريحتين متوسطة", kcal: 560, protein: 24, cat: "main" },
  { id: "fries", name: "بطاطس مقلية", portion: "حجم وسط", kcal: 380, protein: 4, cat: "snack", aliases: ["بطاطس", "فرايز"] },
  { id: "mutabbaq", name: "مطبق لحم", portion: "حبة", kcal: 620, protein: 22, cat: "main", aliases: ["مطبق"] },

  // خبز وفطور
  { id: "khubz-arabic", name: "خبز عربي", portion: "رغيف", kcal: 170, protein: 6, cat: "bread", aliases: ["خبز", "خبزة"] },
  { id: "tamees", name: "خبز تميس", portion: "نص رغيف", kcal: 290, protein: 9, cat: "bread", aliases: ["تميس"] },
  { id: "regag", name: "خبز رقاق", portion: "رغيفين", kcal: 140, protein: 4, cat: "bread", aliases: ["رقاق"] },
  { id: "balaleet", name: "بلاليط", portion: "صحن مع بيض", kcal: 450, protein: 13, cat: "breakfast" },
  { id: "chebab", name: "خبز جباب / فطيرة خليجية", portion: "٢ حبة", kcal: 380, protein: 9, cat: "breakfast", aliases: ["جباب", "خمير"] },
  { id: "eggs-2", name: "بيض مسلوق", portion: "حبتين", kcal: 155, protein: 13, cat: "protein", aliases: ["بيض", "بيضتين"], proteinStar: true },
  { id: "shakshuka", name: "شكشوكة", portion: "٢ بيض", kcal: 260, protein: 14, cat: "breakfast", aliases: ["شكشوكه"] },
  { id: "labneh", name: "لبنة", portion: "ملعقتين كبار", kcal: 110, protein: 6, cat: "breakfast", aliases: ["لبنه"] },
  { id: "cheese-white", name: "جبن أبيض", portion: "٣٠ غ", kcal: 80, protein: 5, cat: "breakfast", aliases: ["جبن", "جبنة"] },
  { id: "zaatar-manakish", name: "مناقيش زعتر", portion: "رغيف", kcal: 420, protein: 9, cat: "breakfast", aliases: ["مناقيش", "زعتر"] },
  { id: "cheese-manakish", name: "مناقيش جبن", portion: "رغيف", kcal: 520, protein: 20, cat: "breakfast" },
  { id: "oats", name: "شوفان بالحليب", portion: "زبدية", kcal: 300, protein: 12, cat: "breakfast", aliases: ["شوفان"] },
  { id: "greek-yogurt", name: "زبادي يوناني", portion: "علبة ١٧٠ غ", kcal: 100, protein: 17, cat: "protein", aliases: ["زبادي", "يوناني"], proteinStar: true },
  { id: "laban", name: "لبن (روب)", portion: "كوب", kcal: 120, protein: 8, cat: "drink", aliases: ["لبن", "روب"] },
  { id: "tuna", name: "تونة بالماء", portion: "علبة", kcal: 120, protein: 26, cat: "protein", aliases: ["تونه"], proteinStar: true },

  // وجبات خفيفة وحلا
  { id: "dates-3", name: "تمر", portion: "٣ حبات", kcal: 70, protein: 1, cat: "fruit", aliases: ["تمرات", "تمره", "رطب"] },
  { id: "luqaimat", name: "لقيمات", portion: "٥ حبات", kcal: 300, protein: 3, cat: "sweet", aliases: ["لقيمات", "عوامة"] },
  { id: "kunafa", name: "كنافة", portion: "قطعة", kcal: 450, protein: 8, cat: "sweet", aliases: ["كنافه"] },
  { id: "basbousa", name: "بسبوسة", portion: "قطعة", kcal: 320, protein: 4, cat: "sweet", aliases: ["هريسة حلا"] },
  { id: "umm-ali", name: "أم علي", portion: "زبدية صغيرة", kcal: 480, protein: 10, cat: "sweet" },
  { id: "kleija", name: "كليجا", portion: "حبتين", kcal: 280, protein: 4, cat: "sweet" },
  { id: "samosa", name: "سمبوسة", portion: "٣ حبات", kcal: 330, protein: 9, cat: "snack", aliases: ["سمبوسه"] },
  { id: "chocolate", name: "شوكولاتة", portion: "لوح صغير ٤٥ غ", kcal: 240, protein: 3, cat: "sweet", aliases: ["شوكلت", "شوكولاته"] },
  { id: "nuts", name: "مكسرات", portion: "حفنة ٣٠ غ", kcal: 180, protein: 6, cat: "snack", aliases: ["لوز", "كاجو", "فستق"] },
  { id: "apple", name: "تفاحة", portion: "حبة", kcal: 95, protein: 0, cat: "fruit", aliases: ["تفاح"] },
  { id: "banana", name: "موزة", portion: "حبة", kcal: 105, protein: 1, cat: "fruit", aliases: ["موز"] },
  { id: "watermelon", name: "بطيخ / حبحب", portion: "صحن", kcal: 85, protein: 2, cat: "fruit", aliases: ["حبحب", "بطيخ"] },

  // مشروبات
  { id: "arabic-coffee", name: "قهوة عربية", portion: "٣ فناجين", kcal: 10, protein: 0, cat: "drink", aliases: ["قهوه", "قهوة"] },
  { id: "karak", name: "شاي كرك", portion: "كوب", kcal: 150, protein: 3, cat: "drink", aliases: ["كرك"] },
  { id: "tea-sugar", name: "شاي بسكر", portion: "استكانة (ملعقتين سكر)", kcal: 35, protein: 0, cat: "drink", aliases: ["شاي", "استكانة"] },
  { id: "latte", name: "لاتيه", portion: "كوب وسط", kcal: 190, protein: 10, cat: "drink", aliases: ["لاتي", "كابتشينو"] },
  { id: "spanish-latte", name: "سبانش لاتيه", portion: "كوب وسط", kcal: 300, protein: 9, cat: "drink", aliases: ["سبانش"] },
  { id: "soft-drink", name: "مشروب غازي", portion: "علبة", kcal: 140, protein: 0, cat: "drink", aliases: ["ببسي", "كولا", "غازي"] },
  { id: "juice", name: "عصير فواكه", portion: "كوب", kcal: 160, protein: 1, cat: "drink", aliases: ["عصير"] },
  { id: "vimto", name: "فيمتو", portion: "كوب", kcal: 110, protein: 0, cat: "drink" },
  { id: "protein-shake", name: "شيك بروتين بالماء", portion: "سكوب", kcal: 120, protein: 24, cat: "protein", aliases: ["بروتين", "واي"], proteinStar: true },
];

const normalize = (s: string) =>
  s
    .replace(/[ً-ْـ]/g, "")
    .replace(/[أإآ]/g, "ا")
    .replace(/ة/g, "ه")
    .replace(/ى/g, "ي")
    .trim()
    .toLowerCase();

export function searchFoods(query: string, limit = 12): Food[] {
  const q = normalize(query);
  if (!q) return [];
  const scored: { f: Food; score: number }[] = [];
  for (const f of FOODS) {
    const names = [f.name, ...(f.aliases ?? [])].map(normalize);
    let score = 0;
    for (const n of names) {
      if (n === q) score = Math.max(score, 100);
      else if (n.startsWith(q)) score = Math.max(score, 70);
      else if (n.includes(q)) score = Math.max(score, 50);
      else if (q.includes(n) && n.length >= 3) score = Math.max(score, 40);
    }
    if (score > 0) scored.push({ f, score });
  }
  return scored.sort((a, b) => b.score - a.score).slice(0, limit).map((s) => s.f);
}

const QUANTITY_WORDS: [RegExp, number][] = [
  [/(نص|نصف)\s/, 0.5],
  [/(ربع)\s/, 0.25],
  [/(صحنين|حبتين|كوبين|اثنين|٢|2)\s/, 2],
  [/(ثلاث|٣|3)\s/, 3],
];

/**
 * يستخرج أطعمة من جملة حرة مثل "تغديت كبسة دجاج ولبن ونص صحن سلطة".
 * يُستخدم في الوضع المحلي (بدون اتصال بالمدرب الذكي).
 */
export function parseMealText(text: string): { food: Food; qty: number }[] {
  const parts = normalize(text)
    .split(/\s+و|،|,|\+|\n| مع /)
    .map((p) => p.trim())
    .filter(Boolean);
  const found: { food: Food; qty: number }[] = [];
  for (const part of parts) {
    let best: Food | null = null;
    let bestLen = 0;
    for (const f of FOODS) {
      for (const n of [f.name, ...(f.aliases ?? [])].map(normalize)) {
        if (n.length >= 2 && part.includes(n) && n.length > bestLen) {
          best = f;
          bestLen = n.length;
        }
      }
    }
    if (best && !found.some((x) => x.food.id === best!.id)) {
      let qty = 1;
      for (const [re, q] of QUANTITY_WORDS) if (re.test(`${part} `)) qty = q;
      found.push({ food: best, qty });
    }
  }
  return found;
}
