package me.mihaidubceac

import io.ktor.server.application.Application

fun Application.rootModule() {
    configureDependencyInjection()
    configureSerialization()
    configureRouting()
}
