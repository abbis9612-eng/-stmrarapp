package app.sanad.core

/**
 * أكلات خليجية وعربية شائعة بحصص واقعية. القيم تقديرية (متوسطات جداول تغذية
 * ووصفات منزلية) — الهدف تسجيل سريع وصادق، لا دقة مخبرية.
 */
enum class FoodCat(val label: String) {
    MAIN("أطباق"), BREAKFAST("فطور"), BREAD("خبز"), PROTEIN("بروتين"), SNACK("خفيف"),
    SWEET("حلا"), DRINK("مشروبات"), FRUIT("فواكه وتمر"),
}

data class Food(
    val id: String,
    val name: String,
    val portion: String,
    val kcal: Int,
    val protein: Int,
    val cat: FoodCat,
    val aliases: List<String> = emptyList(),
    val proteinStar: Boolean = false,
)

val FOODS: List<Food> = listOf(
    // أطباق رئيسية
    Food("kabsa-chicken", "كبسة دجاج", "صحن متوسط (رز + ربع دجاجة)", 650, 38, FoodCat.MAIN, listOf("كبسه", "رز ودجاج", "مكبوس", "مجبوس دجاج", "مجبوس"), false),
    Food("kabsa-meat", "كبسة لحم", "صحن متوسط", 760, 36, FoodCat.MAIN, listOf("رز ولحم", "مجبوس لحم"), false),
    Food("mandi-chicken", "مندي دجاج", "صحن متوسط", 700, 40, FoodCat.MAIN, listOf("مندي"), false),
    Food("madhbi", "مضبي / مشوي على الحجر", "ربع دجاجة + رز قليل", 520, 42, FoodCat.MAIN, emptyList(), true),
    Food("rice-plain", "رز أبيض", "كوب مطبوخ", 205, 4, FoodCat.MAIN, listOf("رز", "عيش"), false),
    Food("harees", "هريس", "صحن متوسط", 420, 24, FoodCat.MAIN, listOf("هريسه"), false),
    Food("jareesh", "جريش", "صحن متوسط", 380, 14, FoodCat.MAIN, emptyList(), false),
    Food("saleeg", "سليق", "صحن متوسط", 560, 28, FoodCat.MAIN, emptyList(), false),
    Food("margoog", "مرقوق / مطازيز", "صحن متوسط", 480, 22, FoodCat.MAIN, listOf("مطازيز", "مرقوق"), false),
    Food("thareed", "ثريد", "صحن متوسط", 520, 26, FoodCat.MAIN, emptyList(), false),
    Food("machboos-fish", "مجبوس سمك", "صحن متوسط", 600, 38, FoodCat.MAIN, listOf("رز وسمك", "صيادية"), false),
    Food("grilled-fish", "سمك مشوي", "قطعة ٢٠٠ غ", 280, 44, FoodCat.PROTEIN, listOf("سمك", "هامور", "كنعد", "صافي"), true),
    Food("shawarma-chicken", "شاورما دجاج", "ساندويتش عادي", 480, 26, FoodCat.MAIN, listOf("شاورما", "شورما"), false),
    Food("shawarma-plate", "صحن شاورما عربي", "صحن مع بطاطس وثوم", 1050, 45, FoodCat.MAIN, listOf("عربي شاورما"), false),
    Food("falafel-sandwich", "ساندويتش فلافل", "ساندويتش", 420, 13, FoodCat.MAIN, listOf("فلافل", "طعمية"), false),
    Food("foul", "فول مدمس", "صحن صغير بزيت قليل", 300, 16, FoodCat.BREAKFAST, listOf("فول"), false),
    Food("hummus", "حمص بطحينة", "صحن صغير", 250, 8, FoodCat.SNACK, listOf("حمص"), false),
    Food("mutabbal", "متبل", "صحن صغير", 180, 4, FoodCat.SNACK, emptyList(), false),
    Food("tabbouleh", "تبولة", "صحن", 170, 3, FoodCat.SNACK, listOf("تبوله"), false),
    Food("fattoush", "فتوش", "صحن", 220, 4, FoodCat.SNACK, listOf("فتوش", "سلطة"), false),
    Food("salad-green", "سلطة خضراء بدون صوص", "صحن كبير", 60, 2, FoodCat.SNACK, listOf("سلطه خضراء", "خضار"), false),
    Food("lentil-soup", "شوربة عدس", "زبدية", 230, 12, FoodCat.MAIN, listOf("شوربه", "عدس"), false),
    Food("oats-soup", "شوربة شوفان", "زبدية", 180, 7, FoodCat.MAIN, emptyList(), false),
    Food("mixed-grill", "مشاوي مشكلة", "٣ أسياخ بدون خبز", 560, 55, FoodCat.PROTEIN, listOf("مشاوي", "كباب", "شيش طاووق", "تكة"), true),
    Food("shish-tawook", "شيش طاووق", "سيخين", 330, 42, FoodCat.PROTEIN, listOf("طاووق"), true),
    Food("chicken-breast", "صدر دجاج مشوي", "١٥٠ غ", 250, 46, FoodCat.PROTEIN, listOf("صدر دجاج", "دجاج مشوي"), true),
    Food("broasted", "بروستد", "٣ قطع + بطاطس", 1150, 55, FoodCat.MAIN, listOf("دجاج مقلي", "بروست"), false),
    Food("burger", "برجر لحم", "ساندويتش متوسط", 550, 28, FoodCat.MAIN, listOf("برقر", "همبرجر"), false),
    Food("pizza", "بيتزا", "شريحتين متوسطة", 560, 24, FoodCat.MAIN, emptyList(), false),
    Food("fries", "بطاطس مقلية", "حجم وسط", 380, 4, FoodCat.SNACK, listOf("بطاطس", "فرايز"), false),
    Food("mutabbaq", "مطبق لحم", "حبة", 620, 22, FoodCat.MAIN, listOf("مطبق"), false),
    // خبز وفطور
    Food("khubz-arabic", "خبز عربي", "رغيف", 170, 6, FoodCat.BREAD, listOf("خبز", "خبزة"), false),
    Food("tamees", "خبز تميس", "نص رغيف", 290, 9, FoodCat.BREAD, listOf("تميس"), false),
    Food("regag", "خبز رقاق", "رغيفين", 140, 4, FoodCat.BREAD, listOf("رقاق"), false),
    Food("balaleet", "بلاليط", "صحن مع بيض", 450, 13, FoodCat.BREAKFAST, emptyList(), false),
    Food("chebab", "خبز جباب / فطيرة خليجية", "٢ حبة", 380, 9, FoodCat.BREAKFAST, listOf("جباب", "خمير"), false),
    Food("eggs-2", "بيض مسلوق", "حبتين", 155, 13, FoodCat.PROTEIN, listOf("بيض", "بيضتين"), true),
    Food("shakshuka", "شكشوكة", "٢ بيض", 260, 14, FoodCat.BREAKFAST, listOf("شكشوكه"), false),
    Food("labneh", "لبنة", "ملعقتين كبار", 110, 6, FoodCat.BREAKFAST, listOf("لبنه"), false),
    Food("cheese-white", "جبن أبيض", "٣٠ غ", 80, 5, FoodCat.BREAKFAST, listOf("جبن", "جبنة"), false),
    Food("zaatar-manakish", "مناقيش زعتر", "رغيف", 420, 9, FoodCat.BREAKFAST, listOf("مناقيش", "زعتر"), false),
    Food("cheese-manakish", "مناقيش جبن", "رغيف", 520, 20, FoodCat.BREAKFAST, emptyList(), false),
    Food("oats", "شوفان بالحليب", "زبدية", 300, 12, FoodCat.BREAKFAST, listOf("شوفان"), false),
    Food("greek-yogurt", "زبادي يوناني", "علبة ١٧٠ غ", 100, 17, FoodCat.PROTEIN, listOf("زبادي", "يوناني"), true),
    Food("laban", "لبن (روب)", "كوب", 120, 8, FoodCat.DRINK, listOf("لبن", "روب"), false),
    Food("tuna", "تونة بالماء", "علبة", 120, 26, FoodCat.PROTEIN, listOf("تونه"), true),
    // وجبات خفيفة وحلا
    Food("dates-3", "تمر", "٣ حبات", 70, 1, FoodCat.FRUIT, listOf("تمرات", "تمره", "رطب"), false),
    Food("luqaimat", "لقيمات", "٥ حبات", 300, 3, FoodCat.SWEET, listOf("لقيمات", "عوامة"), false),
    Food("kunafa", "كنافة", "قطعة", 450, 8, FoodCat.SWEET, listOf("كنافه"), false),
    Food("basbousa", "بسبوسة", "قطعة", 320, 4, FoodCat.SWEET, listOf("هريسة حلا"), false),
    Food("umm-ali", "أم علي", "زبدية صغيرة", 480, 10, FoodCat.SWEET, emptyList(), false),
    Food("kleija", "كليجا", "حبتين", 280, 4, FoodCat.SWEET, emptyList(), false),
    Food("samosa", "سمبوسة", "٣ حبات", 330, 9, FoodCat.SNACK, listOf("سمبوسه"), false),
    Food("chocolate", "شوكولاتة", "لوح صغير ٤٥ غ", 240, 3, FoodCat.SWEET, listOf("شوكلت", "شوكولاته"), false),
    Food("nuts", "مكسرات", "حفنة ٣٠ غ", 180, 6, FoodCat.SNACK, listOf("لوز", "كاجو", "فستق"), false),
    Food("apple", "تفاحة", "حبة", 95, 0, FoodCat.FRUIT, listOf("تفاح"), false),
    Food("banana", "موزة", "حبة", 105, 1, FoodCat.FRUIT, listOf("موز"), false),
    Food("watermelon", "بطيخ / حبحب", "صحن", 85, 2, FoodCat.FRUIT, listOf("حبحب", "بطيخ"), false),
    // مشروبات
    Food("arabic-coffee", "قهوة عربية", "٣ فناجين", 10, 0, FoodCat.DRINK, listOf("قهوه", "قهوة"), false),
    Food("karak", "شاي كرك", "كوب", 150, 3, FoodCat.DRINK, listOf("كرك"), false),
    Food("tea-sugar", "شاي بسكر", "استكانة (ملعقتين سكر)", 35, 0, FoodCat.DRINK, listOf("شاي", "استكانة"), false),
    Food("latte", "لاتيه", "كوب وسط", 190, 10, FoodCat.DRINK, listOf("لاتي", "كابتشينو"), false),
    Food("spanish-latte", "سبانش لاتيه", "كوب وسط", 300, 9, FoodCat.DRINK, listOf("سبانش"), false),
    Food("soft-drink", "مشروب غازي", "علبة", 140, 0, FoodCat.DRINK, listOf("ببسي", "كولا", "غازي"), false),
    Food("juice", "عصير فواكه", "كوب", 160, 1, FoodCat.DRINK, listOf("عصير"), false),
    Food("vimto", "فيمتو", "كوب", 110, 0, FoodCat.DRINK, emptyList(), false),
    Food("protein-shake", "شيك بروتين بالماء", "سكوب", 120, 24, FoodCat.PROTEIN, listOf("بروتين", "واي"), true),
)

fun foodById(id: String): Food? = FOODS.firstOrNull { it.id == id }

fun searchFoods(query: String, limit: Int = 12): List<Food> {
    val q = normalizeArabic(query)
    if (q.isEmpty()) return emptyList()
    return FOODS.mapNotNull { f ->
        val score = (listOf(f.name) + f.aliases).map(::normalizeArabic).maxOf { n ->
            when {
                n == q -> 100
                n.startsWith(q) -> 70
                n.contains(q) -> 50
                q.contains(n) && n.length >= 3 -> 40
                else -> 0
            }
        }
        if (score > 0) f to score else null
    }.sortedByDescending { it.second }.take(limit).map { it.first }
}

data class ParsedFood(val food: Food, val qty: Double)

private val QUANTITY_WORDS = listOf(
    Regex("(نص|نصف)\\s") to 0.5,
    Regex("(ربع)\\s") to 0.25,
    Regex("(صحنين|حبتين|كوبين|اثنين|٢|2)\\s") to 2.0,
    Regex("(ثلاث|٣|3)\\s") to 3.0,
)

/** يستخرج أطعمة من جملة حرة مثل "تغديت كبسة دجاج ولبن ونص صحن سلطة". */
fun parseMealText(text: String): List<ParsedFood> {
    val parts = normalizeArabic(text).split(Regex("\\s+و|،|,|\\+|\n| مع ")).map { it.trim() }.filter { it.isNotEmpty() }
    val found = mutableListOf<ParsedFood>()
    for (part in parts) {
        var best: Food? = null
        var bestLen = 0
        for (f in FOODS) for (n in (listOf(f.name) + f.aliases).map(::normalizeArabic)) {
            if (n.length >= 2 && part.contains(n) && n.length > bestLen) {
                best = f; bestLen = n.length
            }
        }
        val b = best ?: continue
        if (found.any { it.food.id == b.id }) continue
        var qty = 1.0
        for ((re, q) in QUANTITY_WORDS) if (re.containsMatchIn("$part ")) qty = q
        found += ParsedFood(b, qty)
    }
    return found
}
