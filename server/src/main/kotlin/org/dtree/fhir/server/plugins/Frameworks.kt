package org.dtree.fhir.server.plugins

import io.github.cdimascio.dotenv.Dotenv
import io.ktor.server.application.*
import org.dtree.fhir.core.di.FhirProvider
import org.dtree.fhir.server.plugins.injection.ModulesInjection
import org.dtree.fhir.server.services.injection.ServicesInjection
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureFrameworks(dotEnv: Dotenv) {
    install(Koin) {
        slf4jLogger()
        modules(module {
            single { dotEnv }
            single { FhirProvider() }
        }, ModulesInjection.koinBeans, ServicesInjection.koinBeans)
    }
}