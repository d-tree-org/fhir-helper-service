package org.dtree.fhir.server.plugins

import io.ktor.server.application.*
import org.dtree.fhir.server.plugins.injection.ModulesInjection
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureFrameworks() {
    install(Koin) {
        slf4jLogger()
        modules(module {}, ModulesInjection.koinBeans)
    }
}