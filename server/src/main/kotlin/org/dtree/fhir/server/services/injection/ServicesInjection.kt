package org.dtree.fhir.server.services.injection

import org.dtree.fhir.server.services.StatsService
import org.koin.dsl.module

object ServicesInjection {
    val koinBeans = module {
        single<StatsService> { StatsService }
    }
}