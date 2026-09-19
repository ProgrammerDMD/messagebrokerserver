package me.mihaidubceac

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import me.mihaidubceac.model.Message
import me.mihaidubceac.model.MessageAcknowledgment
import me.mihaidubceac.model.User
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

object InMemoryStore {
    val users = Collections.synchronizedList(mutableListOf<User>())
    val messageAcknowledgements = Collections.synchronizedSet(mutableSetOf<MessageAcknowledgment>())
    val messages = Collections.synchronizedList(mutableListOf<Message>())
}

fun main(args: Array<String>) {
    embeddedServer(
        factory = io.ktor.server.netty.Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::rootModule
    ).start(wait = true)
}
