package ie.setu.controllers

import ie.setu.config.JavalinConfig
import kong.unirest.core.HttpResponse
import kong.unirest.core.JsonNode
import kong.unirest.core.Unirest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import ie.setu.domain.db.HealthTips
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HealthTipControllerTest {

    private val origin = "http://localhost:8001"
    private lateinit var app: io.javalin.Javalin

    @BeforeAll
    fun setup() {
        Database.connect(
            "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver"
        )
        transaction {
            SchemaUtils.create(HealthTips)
        }
        app = JavalinConfig().startJavalinService()
    }

    @AfterAll
    fun tearDown() {
        app.stop()
    }

    @BeforeEach
    fun cleanTable() {
        transaction {
            SchemaUtils.drop(HealthTips)
            SchemaUtils.create(HealthTips)
        }
    }

    @Test
    fun `add health tip returns 201`() {
        val response = addHealthTip("Test tip")
        assertEquals(201, response.status)
    }

    @Test
    fun `get all health tips returns 200`() {
        addHealthTip("Tip 1")
        val response = Unirest.get("$origin/api/healthTips").asString()
        assertEquals(200, response.status)
    }

    @Test
    fun `get health tip by id returns 200 after add`() {
        val addResponse = addHealthTip("Tip by id")
        val id = addResponse.body.`object`.getInt("id")
        val response = Unirest.get("$origin/api/healthTips/$id").asString()
        assertEquals(200, response.status)
    }

    @Test
    fun `get health tip by id returns 404 if not found`() {
        val response = Unirest.get("$origin/api/healthTips/99999").asString()
        assertEquals(404, response.status)
    }

    @Test
    fun `add health tip with invalid data returns error`() {
        val response = Unirest.post("$origin/api/healthTips")
            .body("{\"invalidField\":\"no tip\"}")
            .asJson()
        assertTrue(response.status == 400 || response.status == 500)
    }

    @Test
    fun `update health tip returns 200`() {
        val addResponse = addHealthTip("Old tip")
        val id = addResponse.body.`object`.getInt("id")
        val updateResponse = Unirest.patch("$origin/api/healthTips/$id")
            .body("{\"id\":$id, \"tips\":\"New tip\"}")
            .asJson()
        assertEquals(200, updateResponse.status)
    }

    @Test
    fun `update non-existing health tip returns 404`() {
        val updateResponse = Unirest.patch("$origin/api/healthTips/99999")
            .body("{\"id\":99999, \"tips\":\"No such tip\"}")
            .asJson()
        assertEquals(404, updateResponse.status)
    }

    @Test
    fun `delete health tip returns 204`() {
        val addResponse = addHealthTip("Tip to delete")
        val id = addResponse.body.`object`.getInt("id")
        val deleteResponse = deleteHealthTip(id)
        assertEquals(204, deleteResponse.status)
    }

    // Helper functions
    private fun addHealthTip(tips: String): HttpResponse<JsonNode> {
        return Unirest.post("$origin/api/healthTips")
            .body("{\"tips\":\"$tips\"}")
            .asJson()
    }

    private fun deleteHealthTip(id: Int): HttpResponse<String> {
        return Unirest.delete("$origin/api/healthTips/$id").asString()
    }
}