package org.dtree.fhir.server.controller

import org.dtree.fhir.server.services.tracing.TracingListResults
import org.dtree.fhir.server.services.tracing.TracingService
import org.dtree.fhir.server.services.tracing.TracingStatsResults
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate

class TracingControllerImpl : TracingController, BaseController(), KoinComponent {
    private val tracingService by inject<TracingService>()
    override fun getStats(id: String): TracingStatsResults {
        return tracingService.getStats(id)
    }

    override fun getTracingList(facilityId: String, date: LocalDate): TracingListResults {
        val result = tracingService.getTracingList(facilityId, date)
        println(result.results.size)
        return  result
    }
}

interface TracingController {
    fun getStats(id: String): TracingStatsResults

    fun getTracingList(facilityId: String, date: LocalDate) : TracingListResults
}