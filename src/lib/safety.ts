import { bmi } from "./science";

export type SafetyFlag = "under18" | "pregnant" | "eatingDisorder" | "diabetesMeds" | "glp1" | "lowBmi" | "heart";

export interface SafetyResult {
  block: boolean;
  notes: string[];
}

/**
 * فحص أمان قبل إعطاء عجز سعرات. سند مدرب سلوكي، ليس بديلاً عن الطبيب.
 */
export function assessSafety(input: { age: number; weightKg: number; heightCm: number; flags: SafetyFlag[] }): SafetyResult {
  const notes: string[] = [];
  let block = false;
  const b = bmi(input.weightKg, input.heightCm);

  if (input.age < 18 || input.flags.includes("under18")) {
    block = true;
    notes.push("لأقل من ١٨ سنة: الأفضل خطة يشرف عليها طبيب أطفال أو أخصائي تغذية، مو عجز سعرات.");
  }
  if (input.flags.includes("pregnant")) {
    block = true;
    notes.push("أثناء الحمل أو الرضاعة لا ننصح بعجز سعرات. سند يقدر يساعدك بعادات صحية فقط بعد موافقة طبيبتك.");
  }
  if (input.flags.includes("eatingDisorder")) {
    block = true;
    notes.push("مع تاريخ اضطراب أكل، عدّ السعرات قد يضر. نوصي بمختص نفسي/تغذية أولاً.");
  }
  if (b < 20 || input.flags.includes("lowBmi")) {
    block = true;
    notes.push("وزنك ضمن الطبيعي أو أقل؛ التنحيف غير مناسب. ركّز على القوة واللياقة.");
  }
  if (input.flags.includes("diabetesMeds")) {
    notes.push("أدوية السكري (خاصة الإنسولين والسلفونيل يوريا) قد تحتاج تعديل مع تقليل الأكل — راجع طبيبك قبل البدء.");
  }
  if (input.flags.includes("glp1")) {
    notes.push("مع أدوية GLP-1 (مثل أوزمبيك/مونجارو): البروتين وتمارين المقاومة أهم شي لحماية عضلاتك — سند يرفع أولويتهم لك.");
  }
  if (input.flags.includes("heart")) {
    notes.push("مع أمراض القلب أو الضغط غير المنضبط: خذ موافقة طبيبك على التمارين، وابدأ بالمشي.");
  }
  return { block, notes };
}
