package me.mihaidubceac

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.mihaidubceac.model.Message
import me.mihaidubceac.model.User

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello, World!")
        }
        post("/ping") {
            val user = call.receive<User>()
            InMemoryStore.users.add(user)
            call.application.environment.log.info("Added $user")
            call.respond(HttpStatusCode.OK, "Pong!")
        }
        post("/message") {
            val message = call.receive<Message>()
            call.application.environment.log.info("New message $message")
            InMemoryStore.messages.add(message)
        }
        get("/json/kotlinx-serialization") {
            call.respond(mapOf("hello" to "world"))
        }
    }
}