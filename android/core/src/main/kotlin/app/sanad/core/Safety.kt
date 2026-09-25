package app.sanad.core

data class SafetyResult(val block: Boolean, val notes: List<String>)

/** فحص أمان قبل إعطاء عجز سعرات. سند مدرب سلوكي، ليس بديلاً عن الطبيب. */
fun assessSafety(age: Int, weightKg: Double, heightCm: Double, flags: List<SafetyFlag>): SafetyResult {
    val notes = mutableListOf<String>()
    var block = false
    if (age < 18 || SafetyFlag.UNDER18 in flags) {
        block = true
        notes += "لأقل من ١٨ سنة: الأفضل خطة يشرف عليها طبيب أو أخصائي تغذية، مو عجز سعرات."
    }
    if (SafetyFlag.PREGNANT in flags) {
        block = true
        notes += "أثناء الحمل أو الرضاعة لا ننصح بعجز سعرات. سند يساعدك بعادات صحية فقط بعد موافقة طبيبتك."
    }
    if (SafetyFlag.EATING_DISORDER in flags) {
        block = true
        notes += "مع تاريخ اضطراب أكل، عدّ السعرات قد يضر. نوصي بمختص نفسي/تغذية أولاً."
    }
    if (bmi(weightKg, heightCm) < 20 || SafetyFlag.LOW_BMI in flags) {
        block = true
        notes += "وزنك ضمن الطبيعي أو أقل؛ التنحيف غير مناسب. ركّز على القوة واللياقة."
    }
    if (SafetyFlag.DIABETES_MEDS in flags) notes += "أدوية السكري قد تحتاج تعديل مع تقليل الأكل — راجع طبيبك قبل البدء."
    if (SafetyFlag.GLP1 in flags) notes += "مع إبر GLP-1: البروتين وتمارين المقاومة أهم شي لحماية عضلاتك — سند يرفع أولويتهم لك."
    if (SafetyFlag.HEART in flags) notes += "مع أمراض القلب أو الضغط غير المنضبط: خذ موافقة طبيبك على التمارين، وابدأ بالمشي."
    return SafetyResult(block, notes)
}
