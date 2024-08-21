package org.dtree.fhir.server.plugins.injection

import org.dtree.fhir.server.controller.StatsController
import org.dtree.fhir.server.controller.StatsControllerImpl
import org.koin.dsl.module

object ModulesInjection  {
    val koinBeans = module {
        single<StatsController> { StatsControllerImpl() }
    }
}