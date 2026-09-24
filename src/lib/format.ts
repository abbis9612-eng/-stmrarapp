const nf = new Intl.NumberFormat("ar-SA-u-nu-arab", { maximumFractionDigits: 1 });

/** أرقام عربية مشرقية بشكل موحّد في كل الواجهة. */
export const n = (x: number) => nf.format(x);

export function greeting(d = new Date()): string {
  const h = d.getHours();
  if (h < 5) return "سهرانين";
  if (h < 12) return "صباح الخير";
  if (h < 17) return "يعطيك العافية";
  return "مساء الخير";
}

/** يقبل أرقام عربية أو لاتينية وفواصل عشرية بالشكلين. */
export function parseNum(s: string): number {
  const western = s
    .replace(/[٠-٩]/g, (d) => String("٠١٢٣٤٥٦٧٨٩".indexOf(d)))
    .replace(/[٫,]/g, ".")
    .replace(/[^\d.]/g, "");
  return Number(western);
}
