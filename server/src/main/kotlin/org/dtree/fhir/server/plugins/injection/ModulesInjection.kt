package org.dtree.fhir.server.plugins.injection

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.parser.IParser
import io.github.cdimascio.dotenv.Dotenv
import org.dtree.fhir.core.di.FhirProvider
import org.dtree.fhir.core.uploader.general.FhirClient
import org.dtree.fhir.server.controller.*
import org.dtree.fhir.server.services.form.LocalResourceFetcher
import org.dtree.fhir.server.services.form.ResourceFetcher
import org.dtree.fhir.server.services.form.ResponseGenerator
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

object ModulesInjection {
    val koinBeans = module {
        single<IParser> { FhirContext.forCached(FhirVersionEnum.R4).newJsonParser() }

        single<ResourceFetcher> { LocalResourceFetcher() }
        single<StatsController> { StatsControllerImpl() }
        single<TracingController> { TracingControllerImpl() }
        single<AppointmentController> { AppointmentControllerImpl() }
        single<CarePlanController> { CarePlanControllerImpl() }
        single<TasksController> { TasksControllerImpl() }

        single<FhirClient> { FhirClient(this.get<Dotenv>(), this.get<FhirProvider>().parser()) }

        singleOf(::ResponseGenerator)
    }
}