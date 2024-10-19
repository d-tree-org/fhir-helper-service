package org.dtree.fhir.server.services.injection

import org.dtree.fhir.server.services.form.FormService
import org.dtree.fhir.server.services.stats.StatsService
import org.dtree.fhir.server.services.tracing.TracingService
import org.koin.dsl.module

object ServicesInjection {
    val koinBeans = module {
        single<FormService> { FormService }
        single<StatsService> { StatsService }
        single<TracingService> { TracingService }
    }
}