"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { actions, deriveTargets, emptyDay, todayKey, useApp } from "@/lib/store";
import { FOODS, searchFoods, type Food } from "@/lib/foods";
import { dayIntake } from "@/lib/science";
import { n, parseNum } from "@/lib/format";
import { Icon } from "@/components/Icon";
import { useToast } from "@/components/Toast";

const CATS: { id: Food["cat"] | "fav" | "protein+"; label: string }[] = [
  { id: "protein+", label: "غني بالبروتين" },
  { id: "fav", label: "المفضلة" },
  { id: "main", label: "أطباق" },
  { id: "breakfast", label: "فطور" },
  { id: "bread", label: "خبز" },
  { id: "snack", label: "خفيف" },
  { id: "sweet", label: "حلا" },
  { id: "drink", label: "مشروبات" },
  { id: "fruit", label: "فواكه وتمر" },
];

const PORTIONS = [0.5, 1, 1.5, 2];

export default function Eat() {
  const s = useApp();
  const today = todayKey();
  const day = s.days[today] ?? emptyDay(today);
  const t = deriveTargets(s)!;
  const [q, setQ] = useState("");
  const [cat, setCat] = useState<(typeof CATS)[number]["id"]>("protein+");
  const [open, setOpen] = useState<string | null>(null);
  const [custom, setCustom] = useState({ name: "", kcal: "", protein: "" });
  const toast = useToast();

  const results = useMemo(() => {
    if (q.trim()) return searchFoods(q, 20);
    if (cat === "fav") return FOODS.filter((f) => s.favorites.includes(f.id));
    if (cat === "protein+") return FOODS.filter((f) => f.proteinStar);
    return FOODS.filter((f) => f.cat === cat);
  }, [q, cat, s.favorites]);

  const eaten = dayIntake(day);
  const protein = day.meals.reduce((a, m) => a + m.protein, 0);

  const add = (f: Food, qty: number) => {
    actions.addMeal({
      name: qty === 1 ? f.name : `${f.name} ×${n(qty)}`,
      kcal: Math.round(f.kcal * qty),
      protein: Math.round(f.protein * qty),
      source: "db",
    });
    setOpen(null);
    toast.show(`سُجّل: ${f.name}`);
  };

  const customValid = custom.name.trim() && parseNum(custom.kcal) > 0;

  return (
    <div className="stack">
      <header className="spread">
        <h1>الأكل</h1>
        <span className="badge num">
          {n(eaten)} / {n(t.kcal)} سعرة، {n(protein)} غ بروتين
        </span>
      </header>

      <Link href="/coach" className="card row" style={{ textDecoration: "none", color: "inherit" }}>
        <span className="nav-coach" style={{ margin: 0, width: 48, height: 48, flex: "none" }}>
          <Icon name="coach" />
        </span>
        <span className="small">
          <b>أسرع طريقة:</b> اكتب لسند وش أكلت بجملة، ويحسبها لك.
        </span>
      </Link>

      <div className="field">
        <label htmlFor="q" className="sr-only">
          ابحث عن أكلة
        </label>
        <input id="q" className="input" type="search" placeholder="ابحث: كبسة، شاورما، كرك…" value={q} onChange={(e) => setQ(e.target.value)} dir="auto" />
      </div>

      {!q && (
        <div className="chips" style={{ flexWrap: "nowrap", overflowX: "auto", paddingBottom: 4 }}>
          {CATS.map((c) => (
            <button key={c.id} className="chip" style={{ flex: "none" }} aria-pressed={cat === c.id} onClick={() => setCat(c.id)}>
              {c.label}
            </button>
          ))}
        </div>
      )}

      <ul className="list" aria-label="نتائج">
        {results.length === 0 && (
          <li className="note">
            {q ? (
              <>
                ما لقينا "{q}". سجّلها يدوي تحت، أو <Link href={`/coach?q=${encodeURIComponent(`أكلت ${q}`)}`}>اسأل سند يقدّرها</Link>.
              </>
            ) : cat === "fav" ? (
              "اضغط ☆ على أي أكلة تتكرر عندك عشان تلقاها هنا بضغطة."
            ) : null}
          </li>
        )}
        {results.map((f) => {
          const isOpen = open === f.id;
          const fav = s.favorites.includes(f.id);
          return (
            <li key={f.id} className="card" style={{ padding: 0 }}>
              <div className="item" style={{ background: "transparent" }}>
                <button
                  className="item-main"
                  style={{ background: "none", border: 0, textAlign: "start", padding: 0 }}
                  onClick={() => setOpen(isOpen ? null : f.id)}
                  aria-expanded={isOpen}
                >
                  <div className="item-title">{f.name}</div>
                  <div className="xs muted">
                    {f.portion}، <span className="num">{n(f.kcal)}</span> سعرة، <span className="num">{n(f.protein)}</span> غ بروتين
                  </div>
                </button>
                <button className="icon-btn" style={{ background: "none", color: fav ? "var(--date)" : "var(--ink-soft)" }} onClick={() => actions.toggleFavorite(f.id)} aria-pressed={fav} aria-label={fav ? "إزالة من المفضلة" : "أضف للمفضلة"}>
                  <Icon name="star" filled={fav} />
                </button>
              </div>
              {isOpen && (
                <div style={{ padding: "0 14px 14px" }}>
                  <p className="xs muted" style={{ marginBottom: 8 }}>
                    كم أكلت من الحصة؟
                  </p>
                  <div className="chips">
                    {PORTIONS.map((qty) => (
                      <button key={qty} className="chip num" onClick={() => add(f, qty)}>
                        {qty === 1 ? "حصة" : qty === 0.5 ? "نص" : qty === 2 ? "حصتين" : "حصة ونص"} <span className="muted">{n(Math.round(f.kcal * qty))}</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </li>
          );
        })}
      </ul>

      <details className="card">
        <summary style={{ cursor: "pointer", fontWeight: 600, minHeight: 32 }}>إضافة يدوية</summary>
        <form
          className="stack"
          style={{ marginTop: 12 }}
          onSubmit={(e) => {
            e.preventDefault();
            if (!customValid) return;
            actions.addMeal({ name: custom.name.trim(), kcal: Math.round(parseNum(custom.kcal)), protein: Math.round(parseNum(custom.protein) || 0), source: "quick" });
            toast.show(`سُجّل: ${custom.name.trim()}`);
            setCustom({ name: "", kcal: "", protein: "" });
          }}
        >
          <input className="input" placeholder="اسم الأكلة" value={custom.name} onChange={(e) => setCustom({ ...custom, name: e.target.value })} dir="auto" aria-label="اسم الأكلة" />
          <div className="row">
            <input className="input input--num" inputMode="numeric" placeholder="سعرات" value={custom.kcal} onChange={(e) => setCustom({ ...custom, kcal: e.target.value })} aria-label="السعرات" />
            <input className="input input--num" inputMode="numeric" placeholder="بروتين غ" value={custom.protein} onChange={(e) => setCustom({ ...custom, protein: e.target.value })} aria-label="البروتين بالغرام" />
          </div>
          <button className="btn btn--block" disabled={!customValid}>
            سجّل
          </button>
        </form>
      </details>

      <section aria-labelledby="log-t" className="stack" style={{ gap: 8 }}>
        <h2 id="log-t" className="section-title">
          سجل اليوم
        </h2>
        {day.meals.length === 0 ? (
          <p className="muted small">ما سجلت شي اليوم. أول وجبة تسجلها تنجز مهمة الأكل وتنسج صف بخيطك.</p>
        ) : (
          <ul className="list">
            {day.meals.map((m) => (
              <li key={m.id} className="item">
                <div className="item-main">
                  <div className="item-title" dir="auto">
                    {m.name}
                  </div>
                  <div className="xs muted num">
                    {n(m.kcal)} سعرة، {n(m.protein)} غ بروتين
                    {m.source === "coach" && "، قدّرها سند"}
                  </div>
                </div>
                <button className="icon-btn" style={{ background: "none" }} onClick={() => actions.removeMeal(m.id)} aria-label={`احذف ${m.name}`}>
                  <Icon name="trash" size={20} />
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>
      {toast.node}
    </div>
  );
}
