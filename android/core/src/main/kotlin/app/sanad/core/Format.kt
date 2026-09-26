package app.sanad.core

import kotlin.math.abs
import kotlin.math.roundToLong

private const val ARABIC_DIGITS = "٠١٢٣٤٥٦٧٨٩"

/** أرقام عربية مشرقية موحّدة في كل التطبيق، مع فاصل آلاف "٬" وفاصلة عشرية "٫". */
fun ar(x: Number, decimals: Int = 1): String {
    val d = x.toDouble()
    val scale = Math.pow(10.0, decimals.toDouble())
    val rounded = (abs(d) * scale).roundToLong()
    val intPart = rounded / scale.toLong()
    val frac = rounded % scale.toLong()
    val grouped = intPart.toString().reversed().chunked(3).joinToString("٬").reversed()
    val body = if (frac == 0L) grouped else grouped + "٫" + frac.toString().padStart(decimals, '0').trimEnd('0')
    val sign = if (d < 0 && rounded != 0L) "-" else ""
    return sign + body.map { c -> if (c in '0'..'9') ARABIC_DIGITS[c - '0'] else c }.joinToString("")
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
    .replace('گ', 'ك').replace('چ', 'ج').replace('پ', 'ب').replace('ڤ', 'ف')
    .trim()
    .lowercase()
