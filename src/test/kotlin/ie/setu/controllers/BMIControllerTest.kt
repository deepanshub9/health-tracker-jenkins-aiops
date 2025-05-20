package ie.setu.controllers

import ie.setu.config.DbConfig
import ie.setu.domain.Bmi
import ie.setu.domain.User
import ie.setu.helpers.*
import ie.setu.utils.jsonToObject
import kong.unirest.core.HttpResponse
import kong.unirest.core.JsonNode
import kong.unirest.core.Unirest
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BMIControllerTest {

    private val db = DbConfig().getDbConnection()
    private val app = ServerContainer.instance
    private val origin = "http://localhost:" + app.port()

    @Nested
    inner class ReadBMI {
        @Test
        fun `get all bmi records returns 200 or 404`() {
            val response = Unirest.get("$origin/api/bmi").asString()
            if (response.status == 200) {
                val retrievedBmis: ArrayList<Bmi> = jsonToObject(response.body.toString())
                assertNotEquals(0, retrievedBmis.size)
            } else {
                assertEquals(404, response.status)
            }
        }

        @Test
        fun `get bmi by id when not exists returns 404`() {
            val response = Unirest.get("$origin/api/bmi/99999").asString()
            assertEquals(404, response.status)
        }
    }

    @Nested
    inner class CreateBMI {
        @Test
        fun `add bmi for existing user returns 201`() {
            val addUserResponse = addUser(users[0].name, "bmi_test_user@gmail.com")
            val addedUser: User = jsonToObject(addUserResponse.body.toString())
            val addBmiResponse = addBmi(
                70.0, 175.0, addedUser.id, DateTime.now()
            )
            assertEquals(201, addBmiResponse.status)
            deleteUser(addedUser.id)
        }

        @Test
        fun `add bmi for non-existing user returns 404`() {
            val addBmiResponse = addBmi(
                70.0, 175.0, -1, DateTime.now()
            )
            assertEquals(404, addBmiResponse.status)
        }
    }

    @Nested
    inner class GetBMIByUserId {
        @Test
        fun `get bmi by user id returns 200 if exists, 404 if not`() {
            val addUserResponse = addUser(users[0].name, "bmi_test_user2@gmail.com")
            val addedUser: User = jsonToObject(addUserResponse.body.toString())
            val addBmiResponse = addBmi(
                80.0, 180.0, addedUser.id, DateTime.now()
            )
            assertEquals(201, addBmiResponse.status)
            val response = Unirest.get("$origin/api/bmi/users/${addedUser.id}").asString()
            assertEquals(200, response.status)
            deleteUser(addedUser.id)
        }

        @Test
        fun `get bmi by user id returns 404 if user does not exist`() {
            val response = Unirest.get("$origin/api/bmi/users/-1").asString()
            assertEquals(404, response.status)
        }
    }

    @Nested
    inner class DeleteBMI {
        @Test
        fun `delete bmi by id returns 204 if exists, 404 if not`() {
            val addUserResponse = addUser(users[0].name, "bmi_test_user3@gmail.com")
            val addedUser: User = jsonToObject(addUserResponse.body.toString())
            val addBmiResponse = addBmi(
                90.0, 190.0, addedUser.id, DateTime.now()
            )
            assertEquals(201, addBmiResponse.status)
            val addedBmi: Bmi = jsonToObject(addBmiResponse.body.toString())
            val deleteResponse = Unirest.delete("$origin/api/bmi/${addedBmi.id}").asString()
            assertEquals(204, deleteResponse.status)
            val deleteAgainResponse = Unirest.delete("$origin/api/bmi/${addedBmi.id}").asString()
            assertEquals(404, deleteAgainResponse.status)
            deleteUser(addedUser.id)
        }
    }

    // Helper functions
    private fun addUser(name: String, email: String): HttpResponse<JsonNode> {
        return Unirest.post("$origin/api/users")
            .body("{\"name\":\"$name\", \"email\":\"$email\"}")
            .asJson()
    }

    private fun deleteUser(id: Int): HttpResponse<String> {
        return Unirest.delete("$origin/api/users/$id").asString()
    }

    private fun addBmi(
        weight: Double,
        height: Double,
        userId: Int,
        timestamp: DateTime
    ): HttpResponse<JsonNode> {
        return Unirest.post("$origin/api/bmi")
            .body("{\"weight\":$weight, \"height\":$height, \"userId\":$userId, \"timestamp\":\"$timestamp\"}")
            .asJson()
    }
}