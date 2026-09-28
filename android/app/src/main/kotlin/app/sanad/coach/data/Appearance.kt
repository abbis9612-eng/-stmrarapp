package app.sanad.coach.data

import android.content.Context
import app.sanad.coach.ui.theme.Palette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** نموذج الألوان الذي يختاره المستخدم من «المظهر». «ضوء» هو الافتراضي. */
class Appearance(context: Context) {
    private val prefs = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    private val _palette = MutableStateFlow(
        prefs.getString(KEY, null)?.let { n -> Palette.entries.firstOrNull { it.name == n } } ?: Palette.LIGHT,
    )
    val palette: StateFlow<Palette> = _palette

    fun set(p: Palette) {
        prefs.edit().putString(KEY, p.name).apply()
        _palette.value = p
    }

    private companion object { const val KEY = "palette" }
}
