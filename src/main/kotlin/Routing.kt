package me.mihaidubceac

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.mihaidubceac.model.ApiMessageRequest
import me.mihaidubceac.model.Message
import me.mihaidubceac.model.MessageAcknowledgmentRequest
import me.mihaidubceac.model.ApiUserRequest
import java.util.UUID

private const val MAX_ID_LENGTH = 200
private const val MAX_TOPIC_LENGTH = 200
private const val MAX_CONTENT_LENGTH = 10_000

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello, World!")
        }
        post("/message") {
            val receivedMessage = call.receive<ApiMessageRequest>()

            if (receivedMessage.requestId.isBlank() || receivedMessage.requestId.length > MAX_ID_LENGTH) {
                call.respond(HttpStatusCode.BadRequest, "requestId must be non-blank and at most $MAX_ID_LENGTH characters")
                return@post
            }
            if (receivedMessage.userId.isBlank() || receivedMessage.userId.length > MAX_ID_LENGTH) {
                call.respond(HttpStatusCode.BadRequest, "userId must be non-blank and at most $MAX_ID_LENGTH characters")
                return@post
            }
            if (receivedMessage.topic.isBlank() || receivedMessage.topic.length > MAX_TOPIC_LENGTH) {
                call.respond(HttpStatusCode.BadRequest, "topic must be non-blank and at most $MAX_TOPIC_LENGTH characters")
                return@post
            }
            if (receivedMessage.content.isBlank() || receivedMessage.content.length > MAX_CONTENT_LENGTH) {
                call.respond(HttpStatusCode.BadRequest, "content must be non-blank and at most $MAX_CONTENT_LENGTH characters")
                return@post
            }

            val user = Store.findUser(receivedMessage.userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, "User with id ${receivedMessage.userId} not found")
                return@post
            }

            call.application.environment.log.info(
                "New message request: requestId=${receivedMessage.requestId} userId=${receivedMessage.userId} topic=${receivedMessage.topic}"
            )
            val createdMessage = Message(
                messageId = UUID.randomUUID().toString(),
                requestId = receivedMessage.requestId,
                userId = receivedMessage.userId,
                topic = receivedMessage.topic,
                content = receivedMessage.content,
                timestamp = System.currentTimeMillis()
            )

            Store.addMessage(createdMessage)
            call.respond(HttpStatusCode.Created, createdMessage)
        }
        get("/messages/{userId}") {
            val userId = call.parameters["userId"]
            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing userId")
                return@get
            }

            val user = Store.findUser(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, "User with id $userId not found")
                return@get
            }

            call.respond(Store.messagesRemainingFor(user))
        }
        post("/connect") {
            val apiUserRequest = call.receive<ApiUserRequest>()

            if (apiUserRequest.id.isBlank() || apiUserRequest.id.length > MAX_ID_LENGTH) {
                call.respond(HttpStatusCode.BadRequest, "id must be non-blank and at most $MAX_ID_LENGTH characters")
                return@post
            }

            val user = Store.upsertUser(apiUserRequest.id, System.currentTimeMillis())

            call.application.environment.log.info("Connected user ${user.id}")
            call.respond(HttpStatusCode.OK, user)
        }
        post("/acknowledge") {
            val acknowledgment = call.receive<MessageAcknowledgmentRequest>()

            if (acknowledgment.userId.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, "userId must be non-blank")
                return@post
            }
            if (acknowledgment.messageIds.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest, "messageIds must not be empty")
                return@post
            }

            val user = Store.findUser(acknowledgment.userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, "User with id ${acknowledgment.userId} not found")
                return@post
            }

            val existingIds = Store.existingMessageIds(acknowledgment.messageIds)
            val unknownIds = acknowledgment.messageIds.filterNot { it in existingIds }
            if (unknownIds.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, "Unknown messageIds: $unknownIds")
                return@post
            }

            Store.acknowledge(acknowledgment.userId, acknowledgment.messageIds)
            call.application.environment.log.info(
                "Acknowledged ${acknowledgment.messageIds.size} messages for user ${acknowledgment.userId}"
            )
            call.respond(HttpStatusCode.OK)
        }
    }
}
