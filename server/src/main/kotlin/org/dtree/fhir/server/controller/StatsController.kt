package org.dtree.fhir.server.controller

import org.koin.core.component.KoinComponent

class StatsControllerImpl : StatsController, BaseController(), KoinComponent {
    override suspend fun getStats(id: String): String {
      return "Article $id"
    }
}

interface  StatsController {
    suspend fun getStats(id: String): String
}