package app.sanad.core.ai

import app.sanad.core.ChatRole
import app.sanad.core.CoachReply
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * مدرب سند السحابي: التطبيق يكلّم سيرفرنا (/api/v1/coach) والمفتاح يبقى بالسيرفر.
 * كل جهاز يعرّف نفسه بمعرّف تثبيت عشوائي (بدون أي بيانات شخصية) حتى ينحسب حده اليومي.
 * التعليمات والمخطط نفسهم بالسيرفر (منسوخين من CoachPrompt.kt).
 */
class CloudCoach(
    baseUrl: String,
    private val appKey: String,
    private val installId: String,
    private val http: OkHttpClient = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(75, TimeUnit.SECONDS).build(),
) : CoachAI {
    private val url = baseUrl.trim().trimEnd('/') + "/api/v1/coach"
    private val json = Json { ignoreUnknownKeys = true }

    init {
        require(url.startsWith("https://") || url.startsWith("http://localhost") || url.startsWith("http://127.")) { "coach server must be https" }
    }

    fun body(history: List<Turn>, context: String, image: MealImage?): String = buildJsonObject {
        put("messages", buildJsonArray {
            history.takeLast(16).filter { it.text.isNotBlank() }.forEach { t ->
                add(buildJsonObject {
                    put("role", if (t.role == ChatRole.USER) "user" else "coach")
                    put("text", t.text.take(2000))
                })
            }
        })
        put("context", context.take(6000))
        if (image != null) put("image", buildJsonObject { put("mime", image.mime); put("data", image.base64) })
    }.toString()

    override fun reply(history: List<Turn>, context: String, image: MealImage?): CoachReply {
        val req = Request.Builder().url(url)
            .header("x-sanad-install", installId)
            .header("x-sanad-key", appKey)
            .post(body(history, context, image).toRequestBody("application/json".toMediaType()))
            .build()
        val (code, text) = try {
            http.newCall(req).execute().use { it.code to (it.body?.string() ?: "") }
        } catch (e: java.io.IOException) {
            throw CoachException("network", e.message ?: "network error", e)
        }
        if (code in 200..299) return parseCoachJson(text)
        val err = runCatching { (json.parseToJsonElement(text) as JsonObject)["error"]?.jsonPrimitive?.contentOrNull }.getOrNull()
        throw when {
            code == 429 && err == "limit" -> CoachException("limit", "daily limit reached")
            code == 429 -> CoachException("busy", "server busy")
            code == 401 -> CoachException("auth", "app not authorized")
            code == 400 -> CoachException("bad_request", "bad request")
            else -> CoachException("upstream", "HTTP $code ${err.orEmpty()}")
        }
    }
}
