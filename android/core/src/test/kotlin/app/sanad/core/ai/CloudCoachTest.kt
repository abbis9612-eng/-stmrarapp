package app.sanad.core.ai

import app.sanad.core.ChatRole
import app.sanad.core.CoachAction
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CloudCoachTest {
    private var status = 200
    private var response = """{"reply":"هلا","actions":[{"type":"log_water","cups":2}],"remaining":{"messages":39,"photos":8}}"""
    private var lastBody = ""
    private var lastInstall = ""
    private var lastKey = ""
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/api/v1/coach") { ex ->
            lastBody = ex.requestBody.readBytes().toString(Charsets.UTF_8)
            lastInstall = ex.requestHeaders.getFirst("x-sanad-install") ?: ""
            lastKey = ex.requestHeaders.getFirst("x-sanad-key") ?: ""
            val bytes = response.toByteArray()
            ex.sendResponseHeaders(status, bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }
        }
        start()
    }
    private val coach = CloudCoach("http://127.0.0.1:${server.address.port}/", "k1", "3f2b8c1e-9a4d-4c2e-8b7a-1d2e3f4a5b6c")

    @AfterTest fun stop() = server.stop(0)

    @Test fun sendsTurnsContextAndHeaders() {
        val r = coach.reply(listOf(Turn(ChatRole.COACH, "هلا بيك"), Turn(ChatRole.USER, "تغديت دولمة")), "ctx")
        assertEquals("هلا", r.text)
        assertEquals(listOf<CoachAction>(CoachAction.LogWater(2)), r.actions)
        assertEquals("k1", lastKey)
        assertEquals("3f2b8c1e-9a4d-4c2e-8b7a-1d2e3f4a5b6c", lastInstall)
        assertTrue("\"role\":\"coach\"" in lastBody && "دولمة" in lastBody && "\"context\":\"ctx\"" in lastBody)
        assertTrue("image" !in lastBody)
    }

    @Test fun sendsPhoto() {
        coach.reply(listOf(Turn(ChatRole.USER, "صحني")), "", MealImage(ByteArray(300) { 1 }))
        assertTrue("\"mime\":\"image/jpeg\"" in lastBody)
    }

    @Test fun mapsDailyLimit() {
        status = 429; response = """{"error":"limit","reason":"daily_messages"}"""
        assertEquals("limit", assertFailsWith<CoachException> { coach.reply(listOf(Turn(ChatRole.USER, "x")), "") }.code)
    }

    @Test fun mapsUnauthorizedAndUpstream() {
        status = 401; response = """{"error":"unauthorized"}"""
        assertEquals("auth", assertFailsWith<CoachException> { coach.reply(listOf(Turn(ChatRole.USER, "x")), "") }.code)
        status = 503; response = """{"error":"no_key"}"""
        assertEquals("upstream", assertFailsWith<CoachException> { coach.reply(listOf(Turn(ChatRole.USER, "x")), "") }.code)
    }

    @Test fun rejectsPlainHttp() {
        assertFailsWith<IllegalArgumentException> { CloudCoach("http://example.com", "k", "i") }
    }
}
