package me.mihaidubceac

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.mihaidubceac.model.Message
import me.mihaidubceac.model.MessageAcknowledgment
import me.mihaidubceac.model.MessageAcknowledgmentRequest
import me.mihaidubceac.model.User
import me.mihaidubceac.model.UserMessagesQuery

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello, World!")
        }
        post("/message") {
            val message = call.receive<Message>()

            if (InMemoryStore.messages.any { it.id == message.id }) {
                call.respond(HttpStatusCode.Conflict, "Message with id ${message.id} already exists")
                return@post
            }

            call.application.environment.log.info("New $message")
            InMemoryStore.messages.add(message)
            call.respond(HttpStatusCode.Created)
        }
        get("/messages/{userId}") {
            val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")
            if (InMemoryStore.users.none { it.id == userId }) {
                call.respond(HttpStatusCode.Conflict, "User with id ${userId} not found")
                return@get
            }

            val acknowledgedMessages = InMemoryStore.messageAcknowledgements.filter { it.userId == userId }.map { it.messageId }
            val messagesRemaining = InMemoryStore.messages.filter {
                it.id !in acknowledgedMessages
            }

            call.respond(messagesRemaining)
        }
        post("/connect") {
            val user = call.receive<User>()
            if (InMemoryStore.users.any { it.id == user.id }) {
                call.respond(HttpStatusCode.Conflict, "User with id ${user.id} already exists")
                return@post
            }

            InMemoryStore.users.add(user)
            call.application.environment.log.info("Added $user")
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