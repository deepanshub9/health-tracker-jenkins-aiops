package ie.setu.controllers

import ie.setu.config.DbConfig
import ie.setu.domain.Activity
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
class ActivityControllerTest {

    private val db = DbConfig().getDbConnection()
    private val app = ServerContainer.instance
    private val origin = "http://localhost:" + app.port()

    @Nested
    inner class ReadActivity {
        @Test
        fun `get all activities from the database returns 200 or 404 response`() {
            val response = Unirest.get(origin + "/api/activities/").asString()
            if (response.status == 200) {
                val retrievedActivities: ArrayList<Activity> = jsonToObject(response.body.toString())
                assertNotEquals(0, retrievedActivities.size)
            } else {
                assertEquals(404, response.status)
            }
        }

        @Test
        fun `get activity by id when activity does not exist returns 400 response`() {
            val id = -1
            val retrieveResponse = Unirest.get(origin + "/api/activities/$id").asString()
            assertEquals(400, retrieveResponse.status)
        }
    }

    @Nested
    inner class CreateActivities {
        @Test
        fun `add an activity with correct details returns a 201 response`() {
            val addUserResponse = addUser(users[0].name, "test_create_activity@gmail.com")
            val addedUser: User = jsonToObject(addUserResponse.body.toString())
            val addActivityResponse = addActivity(
                activities[0].description,
                activities[0].duration,
                activities[0].calories,
                activities[0].started,
                addedUser.id
            )
            assertEquals(201, addActivityResponse.status)
            deleteUser(addedUser.id)
        }
    }

    @Nested
    inner class UpdateActivity {
        @Test
        fun `updating an activity when it exists returns a 200 response`() {
            val addUserResponse = addUser(users[0].name, "test_update_activity@gmail.com")
            val addedUser: User = jsonToObject(addUserResponse.body.toString())
            val addActivityResponse = addActivity(
                activities[0].description,
                activities[0].duration,
                activities[0].calories,
                activities[0].started,
                addedUser.id
            )
            val addedActivity: Activity = jsonToObject(addActivityResponse.body.toString())
            val activityId = addedActivity.id
            val updatedActivityResponse = updateActivity(
                activityId, "dancing", 60.0, activities[0].calories, activities[0].started, addedUser.id
            )
            assertEquals(200, updatedActivityResponse.status)
            val retrievedActivityResponse = retrieveActivityById(activityId)
            val updatedActivity: Activity = jsonToObject(retrievedActivityResponse.body.toString())
            assertEquals("dancing", updatedActivity.description)
            assertEquals(60.0, updatedActivity.duration)
            deleteUser(addedUser.id)
        }

        @Test
        fun `updating an activity when it doesn't exist returns a 404 response`() {
            assertEquals(
                404,
                updateActivity(-1, activities[0].description, activities[0].duration, activities[0].calories, activities[0].started, -1).status
            )
        }
    }

    @Nested
    inner class DeleteActivities {
        @Test
        fun `deleting an activity when it doesn't exist returns a 404 response`() {
            assertEquals(404, deleteActivity(-1).status)
        }

        @Test
        fun `deleting an activity when it exists returns a 204 response`() {
            val addUserResponse = addUser(users[0].name, "test_delete_activity@gmail.com")
            val addedUser: User = jsonToObject(addUserResponse.body.toString())
            val addActivityResponse = addActivity(
                activities[0].description,
                activities[0].duration,
                activities[0].calories,
                activities[0].started,
                addedUser.id
            )
            val addedActivity: Activity = jsonToObject(addActivityResponse.body.toString())
            val activityId = addedActivity.id
            assertEquals(204, deleteActivity(activityId).status)
            deleteUser(addedUser.id)
        }
    }

    // Helper functions
    private fun addUser(name: String, email: String): HttpResponse<JsonNode> {
        return Unirest.post(origin + "/api/users")
            .body("{\"name\":\"$name\", \"email\":\"$email\"}")
            .asJson()
    }

    private fun deleteUser(id: Int): HttpResponse<String> {
        return Unirest.delete(origin + "/api/users/$id").asString()
    }

    private fun addActivity(
        description: String,
        duration: Double,
        calories: Int,
        started: DateTime,
        userId: Int
    ): HttpResponse<JsonNode> {
        return Unirest.post(origin + "/api/activities")
            .body("{\"description\":\"$description\", \"duration\":\"$duration\", \"calories\":\"$calories\", \"started\":\"$started\", \"userId\":\"$userId\" }")
            .asJson()
    }

    private fun updateActivity(
        id: Int,
        description: String,
        duration: Double,
        calories: Int,
        started: DateTime,
        userId: Int
    ): HttpResponse<JsonNode> {
        return Unirest.patch(origin + "/api/activities/$id")
            .body("{\"description\":\"$description\", \"duration\":\"$duration\", \"calories\":\"$calories\", \"started\":\"$started\", \"userId\":\"$userId\" }")
            .asJson()
    }

    private fun retrieveActivityById(id: Int): HttpResponse<JsonNode> {
        return Unirest.get(origin + "/api/activities/$id").asJson()
    }

    private fun deleteActivity(id: Int): HttpResponse<String> {
        return Unirest.delete(origin + "/api/activities/$id").asString()
    }
}