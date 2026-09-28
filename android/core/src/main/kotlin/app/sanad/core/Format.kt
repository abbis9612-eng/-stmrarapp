package app.sanad.core

import kotlin.math.abs
import kotlin.math.roundToLong

private const val ARABIC_DIGITS = "٠١٢٣٤٥٦٧٨٩"

/** أرقام إنجليزية (0-9) في كل التطبيق، مع فاصل آلاف "," وفاصلة عشرية ".". */
fun ar(x: Number, decimals: Int = 1): String {
    val d = x.toDouble()
    val scale = Math.pow(10.0, decimals.toDouble())
    val rounded = (abs(d) * scale).roundToLong()
    val intPart = rounded / scale.toLong()
    val frac = rounded % scale.toLong()
    val grouped = intPart.toString().reversed().chunked(3).joinToString(",").reversed()
    val body = if (frac == 0L) grouped else grouped + "." + frac.toString().padStart(decimals, '0').trimEnd('0')
    val sign = if (d < 0 && rounded != 0L) "-" else ""
    return sign + body
}

/** رقم بإشارته (+ أو −) داخل عزل من اليسار لليمين، حتى لا تنقلب الإشارة في النص العربي. */
fun arSigned(x: Double, decimals: Int = 1): String {
    val s = when {
        x < 0 && ar(x, decimals) != "0" -> "−" + ar(-x, decimals)
        x > 0 && ar(x, decimals) != "0" -> "+" + ar(x, decimals)
        else -> "0"
    }
    return "\u2066$s\u2069"
}

/** يقبل أرقاماً عربية أو لاتينية وفواصل عشرية بالشكلين. */
fun parseNum(s: String): Double? {
    val western = s.map { c ->
        val i = ARABIC_DIGITS.indexOf(c)
        when {
            i >= 0 -> '0' + i
            c == '٫' || c == ',' -> '.'
            else -> c
        }
    }.joinToString("").filter { it.isDigit() || it == '.' }
    return western.toDoubleOrNull()
}

/** يوحّد الكتابة العربية للبحث: يشيل التشكيل ويوحّد الهمزات والتاء المربوطة. */
fun normalizeArabic(s: String): String = s
    .replace(Regex("[\\u064B-\\u0652\\u0640]"), "")
    .replace(Regex("[أإآ]"), "ا")
    .replace('ة', 'ه')
    .replace('ى', 'ي')
    .map { ch -> ARABIC_DIGITS.indexOf(ch).let { i -> if (i >= 0) '0' + i else ch } }.joinToString("")
    .replace('گ', 'ك').replace('چ', 'ج').replace('پ', 'ب').replace('ڤ', 'ف')
    .trim()
    .lowercase()

/** عدد الأيام بصيغة عربية صحيحة: يوم واحد، يومان، 3 أيام، 11 يوماً. */
fun arDays(n: Int): String = when {
    n == 1 -> "يوم واحد"
    n == 2 -> "يومان"
    n % 100 in 3..10 -> "${ar(n)} أيام"
    else -> "${ar(n)} يوماً"
}
