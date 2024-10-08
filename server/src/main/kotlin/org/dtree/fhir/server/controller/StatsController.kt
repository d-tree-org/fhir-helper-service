package org.dtree.fhir.server.controller

import org.dtree.fhir.server.services.stats.FacilityResultData
import org.dtree.fhir.server.services.stats.StatsService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class StatsControllerImpl : StatsController, BaseController(), KoinComponent {
    private val statsService by inject<StatsService>()

    override suspend fun getStats(id: String): FacilityResultData {
        return statsService.getFacilityStats(id)
    }
}

interface  StatsController {
    suspend fun getStats(id: String): FacilityResultData
}