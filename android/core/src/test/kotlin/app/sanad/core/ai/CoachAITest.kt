package app.sanad.core.ai

import app.sanad.core.ChatRole
import app.sanad.core.CoachAction
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CoachAITest {
    private val history = listOf(Turn(ChatRole.USER, "تغديت مندي دجاج"))

    @Test fun parsesStrictJsonAndDropsInvalidActions() {
        val r = parseCoachJson(
            """```json
            {"reply":"حسبتها ٧٠٠ سعرة","actions":[
              {"type":"log_meal","name":"مندي دجاج","kcal":700,"protein":40},
              {"type":"log_meal","name":"خطأ","kcal":99999,"protein":1},
              {"type":"start_workout","routineId":"not-a-routine"},
              {"type":"log_water","cups":2},
              {"type":"hack","x":1}
            ]}
            ```""",
        )
        assertEquals("حسبتها ٧٠٠ سعرة", r.text)
        assertEquals(listOf(CoachAction.LogMeal("مندي دجاج", 700, 40), CoachAction.LogWater(2)), r.actions)
    }

    @Test fun plainTextFallsBackToReply() {
        val r = parseCoachJson("أهلاً! كيف يومك؟")
        assertEquals("أهلاً! كيف يومك؟", r.text)
        assertTrue(r.actions.isEmpty())
    }

    @Test fun contextIsAttachedToLastUserTurnOnly() {
        val turns = prepareTurns(
            listOf(Turn(ChatRole.COACH, "هلا"), Turn(ChatRole.USER, "أ"), Turn(ChatRole.COACH, "ب"), Turn(ChatRole.USER, "ج")),
            "ctx",
        )
        assertEquals(ChatRole.USER, turns.first().role)
        assertEquals("أ", turns.first().text)
        assertTrue(turns.last().text.startsWith("<app_context>\nctx"))
        assertTrue(turns.last().text.endsWith("ج"))
    }

    @Test fun openAICompatibleProviderRoundTrip() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"choices":[{"message":{"role":"assistant","content":"{\"reply\":\"تمام\",\"actions\":[{\"type\":\"log_meal\",\"name\":\"مندي\",\"kcal\":700,\"protein\":40}]}"}}]}"""))
            val coach = OpenAICompatCoach(server.url("/v1/").toString(), "k-123", "test-model")
            val r = coach.reply(history, "ctx")
            assertEquals("تمام", r.text)
            assertEquals(700, (r.actions.single() as CoachAction.LogMeal).kcal)
            val req = server.takeRequest()
            assertEquals("/v1/chat/completions", req.path)
            assertEquals("Bearer k-123", req.getHeader("Authorization"))
            val body = req.body.readUtf8()
            assertContains(body, "\"response_format\"")
            assertContains(body, "test-model")
            assertContains(body, "app_context")
        }
    }

    @Test fun retriesWithoutJsonModeWhenUnsupported() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"response_format not supported"}"""))
            server.enqueue(MockResponse().setBody("""{"choices":[{"message":{"content":"رد نصي"}}]}"""))
            val r = OpenAICompatCoach(server.url("/").toString(), "k", "m").reply(history, "ctx")
            assertEquals("رد نصي", r.text)
            server.takeRequest()
            assertTrue("response_format" !in server.takeRequest().body.readUtf8())
        }
    }

    @Test fun mapsAuthErrors() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))
            val e = assertFailsWith<CoachException> { OpenAICompatCoach(server.url("/").toString(), "bad", "m").reply(history, "ctx") }
            assertEquals("auth", e.code)
        }
    }

    @Test fun listsModels() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"data":[{"id":"models/gemini-x"},{"id":"alpha"}]}"""))
            assertEquals(listOf("alpha", "gemini-x"), OpenAICompatCoach(server.url("/").toString(), "k", "m").listModels())
        }
    }

    @Test fun claudeRequestUsesSystemPromptAndStructuredOutput() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setHeader("content-type", "application/json").setBody(
                    """{"id":"msg_1","type":"message","role":"assistant","model":"claude-opus-5","stop_reason":"end_turn","stop_sequence":null,
                    "usage":{"input_tokens":10,"output_tokens":10},
                    "content":[{"type":"text","text":"{\"reply\":\"هلا\",\"actions\":[{\"type\":\"log_water\",\"cups\":1}]}"}]}""",
                ),
            )
            val client = AnthropicOkHttpClient.builder().apiKey("sk-test").baseUrl(server.url("/").toString()).build()
            val r = ClaudeCoach(client, "claude-opus-5").reply(history, "ctx")
            assertEquals("هلا", r.text)
            assertEquals(CoachAction.LogWater(1), r.actions.single())
            val body = server.takeRequest().body.readUtf8()
            assertContains(body, "\"system\"")
            assertContains(body, "\"output_config\"")
            assertContains(body, "json_schema")
            assertContains(body, "\"effort\":\"low\"")
        }
    }

    private val photo = MealImage(byteArrayOf(-1, -40, -1, -32, 1, 2, 3), "image/jpeg")

    @Test fun openAICompatibleSendsMealPhotoAsDataUri() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"choices":[{"message":{"content":"{\"reply\":\"كبسة\",\"actions\":[]}"}}]}"""))
            OpenAICompatCoach(server.url("/").toString(), "k", "m").reply(history, "ctx", photo)
            val body = server.takeRequest().body.readUtf8()
            assertContains(body, "\"type\":\"image_url\"")
            assertContains(body, "data:image/jpeg;base64,${photo.base64}")
            assertContains(body, "\"type\":\"text\"")
        }
    }

    @Test fun claudeSendsMealPhotoAsImageBlock() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setHeader("content-type", "application/json").setBody(
                    """{"id":"msg_2","type":"message","role":"assistant","model":"claude-opus-5","stop_reason":"end_turn","stop_sequence":null,
                    "usage":{"input_tokens":10,"output_tokens":10},
                    "content":[{"type":"text","text":"{\"reply\":\"كبسة\",\"actions\":[]}"}]}""",
                ),
            )
            val client = AnthropicOkHttpClient.builder().apiKey("sk-test").baseUrl(server.url("/").toString()).build()
            assertEquals("كبسة", ClaudeCoach(client, "claude-opus-5").reply(history, "ctx", photo).text)
            val body = server.takeRequest().body.readUtf8()
            assertContains(body, "\"type\":\"image\"")
            assertContains(body, "\"media_type\":\"image/jpeg\"")
            assertContains(body, photo.base64)
        }
    }

    @Test fun rejectsUnsupportedImageType() {
        assertFailsWith<IllegalArgumentException> { MealImage(byteArrayOf(1), "image/gif") }
    }
}
