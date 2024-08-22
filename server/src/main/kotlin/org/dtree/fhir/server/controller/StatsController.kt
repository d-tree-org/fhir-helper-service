package org.dtree.fhir.server.controller

import org.dtree.fhir.server.services.StatsService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class StatsControllerImpl : StatsController, BaseController(), KoinComponent {
    private val statsService by inject<StatsService>()

    override suspend fun getStats(id: String): String {
        statsService.getFacilityStats(id)
      return "Article $id"
    }
}

interface  StatsController {
    suspend fun getStats(id: String): String
}