package org.dtree.fhir.server.controller

import org.dtree.fhir.server.services.ResultData
import org.dtree.fhir.server.services.StatsService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class StatsControllerImpl : StatsController, BaseController(), KoinComponent {
    private val statsService by inject<StatsService>()

    override suspend fun getStats(id: String): ResultData {
        return statsService.getFacilityStats(id)
    }
}

interface  StatsController {
    suspend fun getStats(id: String): ResultData
}