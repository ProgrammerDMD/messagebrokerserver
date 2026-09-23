package me.mihaidubceac

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import me.mihaidubceac.db.DatabaseFactory
import me.mihaidubceac.rootModule

fun main(args: Array<String>) {
    DatabaseFactory.init()

    embeddedServer(
        factory = io.ktor.server.netty.Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::rootModule
    ).start(wait = true)
}
