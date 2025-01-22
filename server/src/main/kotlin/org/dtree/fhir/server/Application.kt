package org.dtree.fhir.server

import io.ktor.server.application.*
import org.dtree.fhir.server.plugins.configureFrameworks
import org.dtree.fhir.server.plugins.configureRouting
import org.dtree.fhir.server.plugins.configureScheduler
import org.dtree.fhir.server.plugins.configureSecurity
import org.dtree.fhir.server.util.loadEnv

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val dotEnv = loadEnv()

    configureFrameworks(dotEnv)
    configureSecurity()
    configureRouting()

    configureScheduler()
}
