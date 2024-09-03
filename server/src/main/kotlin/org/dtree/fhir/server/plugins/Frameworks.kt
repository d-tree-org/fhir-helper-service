package org.dtree.fhir.server.plugins

import io.github.cdimascio.dotenv.Dotenv
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import org.dtree.fhir.core.di.FhirProvider
import org.dtree.fhir.server.plugins.injection.ModulesInjection
import org.dtree.fhir.server.services.injection.ServicesInjection
import org.dtree.fhir.server.util.LocalDateAdapter
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import java.time.LocalDate

fun Application.configureFrameworks(dotEnv: Dotenv) {
    install(ContentNegotiation) {
        gson {
            registerTypeAdapter(LocalDate::class.java,LocalDateAdapter())
        }
    }
    install(Koin) {
        slf4jLogger()
        modules(module {
            single { dotEnv }
            single { FhirProvider() }
        }, ModulesInjection.koinBeans, ServicesInjection.koinBeans)
    }
}