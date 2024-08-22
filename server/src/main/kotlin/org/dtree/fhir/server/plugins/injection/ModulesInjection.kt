package org.dtree.fhir.server.plugins.injection

import io.github.cdimascio.dotenv.Dotenv
import org.dtree.fhir.core.di.FhirProvider
import org.dtree.fhir.core.uploader.general.FhirClient
import org.dtree.fhir.server.controller.StatsController
import org.dtree.fhir.server.controller.StatsControllerImpl
import org.koin.dsl.module

object ModulesInjection  {
    val koinBeans = module {
        single<StatsController> { StatsControllerImpl() }
        single<FhirClient> {  FhirClient(this.get<Dotenv>(), this.get<FhirProvider>().parser()) }
    }
}