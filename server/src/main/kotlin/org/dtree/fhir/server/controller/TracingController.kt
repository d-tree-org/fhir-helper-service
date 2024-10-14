package org.dtree.fhir.server.controller

import org.dtree.fhir.server.services.tracing.AppointmentListResults
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

    override fun getAppointmentList(facilityId: String, date: LocalDate): AppointmentListResults {
        return tracingService.getAppointmentList(facilityId, date)
    }
}

interface TracingController {
    fun getStats(id: String): TracingStatsResults

    fun getAppointmentList(facilityId: String, date: LocalDate) : AppointmentListResults
}