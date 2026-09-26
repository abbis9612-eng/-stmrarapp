package app.sanad.core

/**
 * شريك المتابعة: "المساءلة الداعمة" — وجود شخص يعرف بتقدمك يرفع الالتزام.
 * بدون سيرفر: تقرير أسبوعي قصير يرسله المستخدم بنفسه (واتساب أو غيره)،
 * والوزن ما يطلع إلا إذا المستخدم سمح.
 */
fun partnerReport(p: Profile, r: WeeklyReview, streak: Int, showWeight: Boolean): String = buildString {
    val name = p.name.ifBlank { "صديقك" }
    appendLine("تقرير ${name} الأسبوعي من «سند» 🌿")
    appendLine("• حضر ${ar(r.activeDays)} من ٧ أيام")
    if (r.workouts > 0) appendLine("• تمرّن ${ar(r.workouts)} مرات (${ar(r.workoutMinutes)} دقيقة)")
    if (r.proteinDays > 0) appendLine("• وصل هدف البروتين ${ar(r.proteinDays)} أيام")
    if (streak >= 2) appendLine("• سلسلة الاستمرار: ${ar(streak)} يوم")
    if (showWeight) r.trendChangeKg?.let { d ->
        appendLine(if (d <= 0) "• الاتجاه نزل ${ar(-d)} كغ" else "• الاتجاه ثابت تقريباً هالأسبوع")
    }
    // إنجاز الأسبوع ممكن يذكر الوزن؛ إذا الوزن مخفي نعطي إنجاز الحضور بداله
    val win = if (!showWeight && ("كغ" in r.win || "وزن" in r.win)) "رجع لخطته ${ar(r.activeDays)} أيام هالأسبوع" else r.win
    appendLine("إنجاز الأسبوع: $win")
    append("شجّعه برسالة 💬 — التشجيع يفرق أكثر مما تتصور.")
}
