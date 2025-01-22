package org.dtree.fhir.server.plugins

import io.github.cdimascio.dotenv.Dotenv
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import org.dtree.fhir.core.di.FhirProvider
import org.dtree.fhir.server.data.JobHistoryRepository
import org.dtree.fhir.server.plugins.injection.ModulesInjection
import org.dtree.fhir.server.plugins.scheduler.JobFactory
import org.dtree.fhir.server.plugins.scheduler.JobSchedulerManager
import org.dtree.fhir.server.services.injection.ServicesInjection
import org.dtree.fhir.server.services.tracing.TracingService
import org.dtree.fhir.server.util.LocalDateAdapter
import org.dtree.fhir.server.util.LocalDateTimeTypeAdapter
import org.jetbrains.exposed.sql.Database
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import java.time.LocalDate
import java.time.LocalDateTime

fun Application.configureFrameworks(dotEnv: Dotenv) {
    install(ContentNegotiation) {
        gson {
            registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
            registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeTypeAdapter())
        }
    }
    install(Koin) {
        slf4jLogger(level = Level.DEBUG)
        modules(module {
            single { dotEnv }
            single { FhirProvider() }
            single<Database>(createdAtStart = true) {
                Database.connect(
                    url = dotEnv["DB_URL"] ?: "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
                    user = dotEnv["DB_USER"] ?: "sa",
                    driver = dotEnv["DB_DRIVER"] ?: "org.h2.Driver",
                    password = dotEnv["DB_PASSWORD"] ?: "",
                )
            }
        }, ModulesInjection.koinBeans, ServicesInjection.koinBeans, module {
            single<JobHistoryRepository> {
                JobHistoryRepository(this.get<Database>())
            }
            single { JobSchedulerManager(dotEnv, JobFactory(this.get<TracingService>(), this.get())) }
        })

    }
}