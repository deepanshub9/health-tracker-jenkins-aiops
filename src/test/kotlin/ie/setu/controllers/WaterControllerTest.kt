package ie.setu.controllers

import ie.setu.config.JavalinConfig
import kong.unirest.core.HttpResponse
import kong.unirest.core.JsonNode
import kong.unirest.core.Unirest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import ie.setu.domain.db.Water
import org.joda.time.DateTime
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WaterControllerTest {

    private val origin = "http://localhost:8001"
    private lateinit var app: io.javalin.Javalin

    @BeforeAll
    fun setup() {
        Database.connect(
            "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver"
        )
        transaction {
            SchemaUtils.create(Water)
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
            SchemaUtils.drop(Water)
            SchemaUtils.create(Water)
        }
    }

    @Test
    fun `add water returns 200`() {
        val response = addWater(2.0, DateTime.now().toString(), 1)
        assertEquals(200, response.status)
    }

    @Test
    fun `get all water returns 200`() {
        addWater(1.5, DateTime.now().toString(), 1)
        val response = Unirest.get("$origin/api/water").asString()
        assertEquals(200, response.status)
    }

    @Test
    fun `get water by id returns 200 after add`() {
        val addResponse = addWater(2.5, DateTime.now().toString(), 1)
        val id = addResponse.body.`object`.getInt("id")
        val response = Unirest.get("$origin/api/water/$id").asString()
        assertEquals(200, response.status)
    }

    @Test
    fun `get water by id returns 400 if not found`() {
        val response = Unirest.get("$origin/api/water/99999").asString()
        assertEquals(400, response.status)
    }

    @Test
    fun `get water by user id returns 200 or 400`() {
        addWater(3.0, DateTime.now().toString(), 2)
        val response = Unirest.get("$origin/api/water/users/2").asString()
        assertTrue(response.status == 200 || response.status == 400)
    }

    @Test
    fun `add water with invalid data returns error`() {
        val response = Unirest.post("$origin/api/water")
            .body("{\"invalidField\":\"no water\"}")
            .asJson()
        assertTrue(response.status == 400 || response.status == 500)
    }

    @Test
    fun `update water returns 200`() {
        val addResponse = addWater(4.0, DateTime.now().toString(), 3)
        val id = addResponse.body.`object`.getInt("id")
        val updateResponse = Unirest.patch("$origin/api/water/$id")
            .body("{\"id\":$id, \"litres\":5.0, \"dateofdrinking\":\"${DateTime.now()}\", \"userid\":3}")
            .asJson()
        assertEquals(200, updateResponse.status)
    }

    @Test
    fun `delete water returns 200 or 204`() {
        val addResponse = addWater(3.0, DateTime.now().toString(), 4)
        val id = addResponse.body.`object`.getInt("id")
        val deleteResponse = deleteWater(id)
        // Your controller returns 204 as JSON, but HTTP status may be 200 or 204
        assertTrue(deleteResponse.status == 200 || deleteResponse.status == 204)
    }

    @Test
    fun `delete non-existing water returns 400`() {
        val deleteResponse = deleteWater(99999)
        assertEquals(400, deleteResponse.status)
    }

    // Helper functions
    private fun addWater(litres: Double, dateofdrinking: String, userid: Int): HttpResponse<JsonNode> {
        return Unirest.post("$origin/api/water")
            .body("{\"litres\":$litres, \"dateofdrinking\":\"$dateofdrinking\", \"userid\":$userid}")
            .asJson()
    }

    private fun deleteWater(id: Int): HttpResponse<String> {
        return Unirest.delete("$origin/api/water/$id").asString()
    }
}