package org.dtree.fhir.fhirService

import io.ktor.server.application.*
import org.dtree.fhir.fhirService.plugins.configureRouting
import org.dtree.fhir.fhirService.plugins.configureSecurity

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSecurity()
    configureRouting()
}
