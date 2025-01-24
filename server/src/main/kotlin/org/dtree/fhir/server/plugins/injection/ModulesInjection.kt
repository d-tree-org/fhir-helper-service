package org.dtree.fhir.server.plugins.injection

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.datacapture.XFhirQueryResolver
import io.github.cdimascio.dotenv.Dotenv
import org.dtree.fhir.core.di.DXFhirQueryResolver
import org.dtree.fhir.core.di.FhirProvider
import org.dtree.fhir.core.uploader.general.FhirClient
import org.dtree.fhir.server.controller.*
import org.dtree.fhir.server.services.form.LocalResourceFetcher
import org.dtree.fhir.server.services.form.ResourceFetcher
import org.dtree.fhir.server.services.form.ResponseGenerator
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

object ModulesInjection {
    val koinBeans = module {
        single<XFhirQueryResolver> { DXFhirQueryResolver() }
        single<IParser>(createdAtStart = true) { FhirContext.forCached(FhirVersionEnum.R4).newJsonParser() }

        single<ResourceFetcher>(createdAtStart = true) {
            val fetcher = LocalResourceFetcher(this.get<Dotenv>(), this.get<FhirProvider>().parser, this.get())
            fetcher.getRepository()
            fetcher
        }
        singleOf(::StatsControllerImpl) { bind<StatsController>() }
        singleOf(::TracingControllerImpl) { bind<TracingController>() }
        singleOf(::AppointmentControllerImpl) { bind<AppointmentController>() }
        singleOf(::CarePlanControllerImpl) { bind<CarePlanController>() }
        singleOf(::TasksControllerImpl) { bind<TasksController>() }
        singleOf(::PatientControllerImpl) { bind<PatientController>() }

        singleOf(::FhirClient)
        singleOf(::ResponseGenerator)
    }
}