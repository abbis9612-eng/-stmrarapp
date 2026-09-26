package app.sanad.core.ai

import app.sanad.core.ChatRole
import app.sanad.core.CoachReply
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.messages.Base64ImageSource
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.ImageBlockParam
import com.anthropic.models.messages.JsonOutputFormat
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** Claude عبر مكتبة Anthropic الرسمية، مع مخرجات منظّمة (JSON schema) وجهد منخفض لسرعة المحادثة. */
class ClaudeCoach(private val client: AnthropicClient, private val model: String) : CoachAI {
    constructor(apiKey: String, model: String) : this(AnthropicOkHttpClient.builder().apiKey(apiKey).build(), model)

    private val schema: JsonOutputFormat.Schema by lazy {
        @Suppress("UNCHECKED_CAST")
        val map = ObjectMapper().readValue(COACH_SCHEMA_JSON, Map::class.java) as Map<String, Any?>
        JsonOutputFormat.Schema.builder().putAllAdditionalProperties(map.mapValues { JsonValue.from(it.value) }).build()
    }

    override fun reply(history: List<Turn>, context: String, image: MealImage?): CoachReply {
        val b = MessageCreateParams.builder()
            .model(model.ifBlank { "claude-opus-5" })
            .maxTokens(4000L)
            .system(COACH_SYSTEM)
            .outputConfig(
                OutputConfig.builder()
                    .effort(OutputConfig.Effort.LOW)
                    .format(JsonOutputFormat.builder().schema(schema).build())
                    .build(),
            )
        val turns = prepareTurns(history, context)
        turns.forEachIndexed { i, t ->
            when {
                t.role != ChatRole.USER -> b.addAssistantMessage(t.text)
                image != null && i == turns.lastIndex -> b.addUserMessageOfBlockParams(
                    listOf(
                        ContentBlockParam.ofImage(
                            ImageBlockParam.builder().source(
                                Base64ImageSource.builder().data(image.base64).mediaType(
                                    when (image.mime) {
                                        "image/png" -> Base64ImageSource.MediaType.IMAGE_PNG
                                        "image/webp" -> Base64ImageSource.MediaType.IMAGE_WEBP
                                        else -> Base64ImageSource.MediaType.IMAGE_JPEG
                                    },
                                ).build(),
                            ).build(),
                        ),
                        ContentBlockParam.ofText(t.text),
                    ),
                )
                else -> b.addUserMessage(t.text)
            }
        }
        val res = try {
            client.messages().create(b.build())
        } catch (e: UnauthorizedException) {
            throw CoachException("auth", "invalid key", e)
        } catch (e: RateLimitException) {
            throw CoachException("busy", "rate limited", e)
        } catch (e: AnthropicServiceException) {
            throw CoachException("upstream", e.message ?: "service error", e)
        }
        if (res.stopReason().orElse(null) == StopReason.REFUSAL) {
            return CoachReply("ما أقدر أساعد في هذا الطلب. لو عندك سؤال عن أكلك أو حركتك اليوم، أنا موجود.", emptyList())
        }
        val text = res.content().mapNotNull { it.text().orElse(null)?.text() }.joinToString("")
        return parseCoachJson(text)
    }
}

/**
 * أي مزوّد متوافق مع OpenAI (Gemini، Groq، OpenRouter، …) عبر /chat/completions.
 * نطلب JSON بـ response_format، ولو المزوّد رفضه نعيد بدون.
 */
class OpenAICompatCoach(baseUrl: String, private val apiKey: String, private val model: String) : CoachAI {
    private val base = baseUrl.trim().let { if (it.endsWith("/")) it else "$it/" }
    private val http = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS).build()
    private val json = Json { ignoreUnknownKeys = true }
    private val mediaJson = "application/json".toMediaType()

    private fun body(turns: List<Turn>, jsonMode: Boolean, image: MealImage? = null) = buildJsonObject {
        put("model", model)
        put("temperature", 0.6)
        put("messages", buildJsonArray {
            add(buildJsonObject { put("role", "system"); put("content", COACH_SYSTEM + "\n" + JSON_INSTRUCTIONS) })
            turns.forEachIndexed { i, t ->
                add(buildJsonObject {
                    put("role", if (t.role == ChatRole.USER) "user" else "assistant")
                    if (image != null && i == turns.lastIndex && t.role == ChatRole.USER) {
                        // صيغة الصور في واجهة OpenAI: نص + image_url بصيغة data URI
                        put("content", buildJsonArray {
                            add(buildJsonObject { put("type", "text"); put("text", t.text) })
                            add(buildJsonObject {
                                put("type", "image_url")
                                put("image_url", buildJsonObject { put("url", "data:${image.mime};base64,${image.base64}") })
                            })
                        })
                    } else put("content", t.text)
                })
            }
        })
        if (jsonMode) put("response_format", buildJsonObject { put("type", "json_object") })
    }.toString()

    private fun post(payload: String): Pair<Int, String> {
        val req = Request.Builder().url("${base}chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(payload.toRequestBody(mediaJson))
            .build()
        return try {
            http.newCall(req).execute().use { it.code to (it.body?.string() ?: "") }
        } catch (e: java.io.IOException) {
            throw CoachException("network", e.message ?: "network error", e)
        }
    }

    override fun reply(history: List<Turn>, context: String, image: MealImage?): CoachReply {
        require(base.startsWith("https://") || base.startsWith("http://localhost") || base.startsWith("http://127.")) { "base url must be https" }
        val turns = prepareTurns(history, context)
        var (code, text) = post(body(turns, jsonMode = true, image))
        if (code == 400 && text.contains("response_format", ignoreCase = true)) {
            val retry = post(body(turns, jsonMode = false, image)); code = retry.first; text = retry.second
        }
        when (code) {
            in 200..299 -> Unit
            401, 403 -> throw CoachException("auth", "invalid key ($code)")
            429 -> throw CoachException("busy", "rate limited")
            else -> throw CoachException("upstream", "HTTP $code: ${text.take(200)}")
        }
        val content = runCatching {
            val root = json.parseToJsonElement(text).jsonObject
            ((root["choices"] as JsonArray)[0].jsonObject["message"] as JsonObject)["content"]!!.jsonPrimitive.content
        }.getOrElse { throw CoachException("bad_output", "unexpected response shape", it) }
        return parseCoachJson(content)
    }

    override fun listModels(): List<String> {
        val req = Request.Builder().url("${base}models").header("Authorization", "Bearer $apiKey").get().build()
        return try {
            http.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return emptyList()
                val root = json.parseToJsonElement(res.body?.string().orEmpty()).jsonObject
                (root["data"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.get("id")?.let { id -> (id as? JsonPrimitive)?.contentOrNull } }
                    .map { it.removePrefix("models/") }.sorted()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
