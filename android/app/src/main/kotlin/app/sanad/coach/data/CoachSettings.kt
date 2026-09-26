package app.sanad.coach.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import app.sanad.coach.BuildConfig
import app.sanad.core.ai.CloudCoach
import app.sanad.core.ai.CoachAI
import app.sanad.core.ai.PRESETS
import app.sanad.core.ai.createCoach
import app.sanad.core.ai.ProviderConfig
import app.sanad.core.ai.ProviderPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** يشفّر المفتاح بمفتاح AES داخل Android Keystore؛ المفتاح نفسه ما يطلع من الجهاز. */
private object Vault {
    private const val ALIAS = "sanad_coach_key"

    private fun secret(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return gen.generateKey()
    }

    fun encrypt(plain: String): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, secret()) }
        val out = c.iv + c.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    fun decrypt(enc: String): String? = runCatching {
        val all = Base64.decode(enc, Base64.NO_WRAP)
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, secret(), GCMParameterSpec(128, all, 0, 12)) }
        String(c.doFinal(all, 12, all.size - 12), Charsets.UTF_8)
    }.getOrNull()
}

data class CoachSettingsState(val preset: ProviderPreset, val model: String, val baseUrl: String, val hasKey: Boolean) {
    val label: String get() = preset.label
}

/** إعدادات المدرب الذكي: المزوّد والنموذج والمفتاح (مشفّر). */
class CoachSettings(context: Context) {
    private val prefs = context.getSharedPreferences("coach", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<CoachSettingsState?> = _state.asStateFlow()

    private fun read(): CoachSettingsState? {
        val id = prefs.getString("preset", null) ?: return null
        val preset = PRESETS.firstOrNull { it.id == id } ?: return null
        val hasKey = prefs.getString("key", null)?.let(Vault::decrypt)?.isNotBlank() == true
        return CoachSettingsState(preset, prefs.getString("model", preset.defaultModel).orEmpty(), prefs.getString("base", preset.baseUrl).orEmpty(), hasKey)
    }

    fun config(): ProviderConfig? {
        val s = _state.value ?: return null
        val key = prefs.getString("key", null)?.let(Vault::decrypt)?.takeIf { it.isNotBlank() } ?: return null
        return ProviderConfig(s.preset.kind, key, s.model, s.baseUrl)
    }

    /** معرّف تثبيت عشوائي (بدون أي بيانات شخصية) يحسب حد المدرب السحابي اليومي لهذا الجهاز. */
    val installId: String
        get() = prefs.getString("install", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("install", it).apply() }

    /** مدرب سند السحابي متوفر بهذه النسخة (السيرفر مضبوط وقت البناء). */
    val cloudAvailable: Boolean get() = BuildConfig.SANAD_API_URL.isNotBlank()

    /** المدرب الذكي: مفتاح المستخدم الخاص أولاً إن وجد، وإلا سند السحابي، وإلا null (المدرب المحلي). */
    fun coach(): CoachAI? =
        config()?.let(::createCoach)
            ?: if (cloudAvailable) CloudCoach(BuildConfig.SANAD_API_URL, BuildConfig.SANAD_APP_KEY, installId) else null

    fun save(preset: ProviderPreset, model: String, baseUrl: String, apiKey: String?) {
        prefs.edit().apply {
            putString("preset", preset.id)
            putString("model", model.trim())
            putString("base", baseUrl.trim())
            if (!apiKey.isNullOrBlank()) putString("key", Vault.encrypt(apiKey.trim()))
        }.apply()
        _state.value = read()
    }

    fun clear() {
        prefs.edit().clear().apply()
        _state.value = null
    }
}
