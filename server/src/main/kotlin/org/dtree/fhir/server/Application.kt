package org.dtree.fhir.server

import io.ktor.server.application.*
import org.dtree.fhir.server.plugins.configureFrameworks
import org.dtree.fhir.server.plugins.configureRouting
import org.dtree.fhir.server.plugins.configureSecurity

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSecurity()
    configureRouting()
    configureFrameworks()
}
