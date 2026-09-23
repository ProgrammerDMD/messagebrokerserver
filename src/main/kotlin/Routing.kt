package me.mihaidubceac

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.mihaidubceac.model.ApiMessageRequest
import me.mihaidubceac.model.Message
import me.mihaidubceac.model.MessageAcknowledgment
import me.mihaidubceac.model.MessageAcknowledgmentRequest
import me.mihaidubceac.model.ApiUserRequest
import me.mihaidubceac.model.User
import java.util.UUID

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello, World!")
        }
        post("/message") {
            val receivedMessage = call.receive<ApiMessageRequest>()

            call.application.environment.log.info("New message request: $receivedMessage")
            val createdMessage = Message(
                messageId = UUID.randomUUID().toString(),
                requestId = receivedMessage.requestId,
                userId = receivedMessage.userId,
                topic = receivedMessage.topic,
                content = receivedMessage.content,
                timestamp = System.currentTimeMillis()
            )

            InMemoryStore.messages.add(createdMessage)
            call.respond(HttpStatusCode.Created, createdMessage)
        }
        get("/messages/{userId}") {
            val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")

            val user = InMemoryStore.users.find { it.id == userId }
            if (user == null) {
                call.respond(HttpStatusCode.Conflict, "User with id $userId not found")
                return@get
            }

            val acknowledgedMessages = InMemoryStore.messageAcknowledgements.filter { it.userId == userId }.map { it.messageId }
            val messagesRemaining = InMemoryStore.messages.filter {
                it.messageId !in acknowledgedMessages && it.timestamp > user.createdAt
            }

            call.respond(messagesRemaining)
        }
        post("/connect") {
            val apiUserRequest = call.receive<ApiUserRequest>()
            if (InMemoryStore.users.any { it.id == apiUserRequest.id }) {
                call.respond(HttpStatusCode.Conflict, "User with id ${apiUserRequest.id} already exists")
                return@post
            }

            InMemoryStore.users.add(
                User(
                    id = apiUserRequest.id,
                    createdAt = System.currentTimeMillis()
                )
            )

            call.application.environment.log.info("Added $apiUserRequest")
            call.respond(HttpStatusCode.OK)
        }
        post("/acknowledge") {
            val acknowledgment = call.receive<MessageAcknowledgmentRequest>()
            if (InMemoryStore.users.none { it.id == acknowledgment.userId }) {
                call.respond(HttpStatusCode.Conflict, "User with id ${acknowledgment.userId} not found")
                return@post
            }

            acknowledgment.messageIds.forEach {
                InMemoryStore.messageAcknowledgements.add(
                    MessageAcknowledgment(
                        userId = acknowledgment.userId,
                        messageId = it
                    )
                )
            }
            call.application.environment.log.info("Acknowledged $acknowledgment")
            call.respond(HttpStatusCode.OK)
        }
    }
}