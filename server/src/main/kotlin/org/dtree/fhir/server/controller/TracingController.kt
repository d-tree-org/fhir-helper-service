package org.dtree.fhir.server.controller

import org.dtree.fhir.server.core.models.PaginatedResponse
import org.dtree.fhir.server.core.models.PaginationArgs
import org.dtree.fhir.server.services.tracing.TracingResult
import org.dtree.fhir.server.services.tracing.TracingService
import org.dtree.fhir.server.services.tracing.TracingStatsResults
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TracingControllerImpl : TracingController, BaseController(), KoinComponent {
    private val tracingService by inject<TracingService>()
    override fun getStats(id: String): TracingStatsResults {
        return tracingService.getStats(id)
    }

    override suspend fun getTracingList(
        facilityId: String,
        pagination: PaginationArgs
    ): PaginatedResponse<TracingResult> {
        val result = tracingService.getTracingList(facilityId, pagination)
        return result
    }

    override suspend fun setPatientsEnteredInError(patients: List<String>): Boolean {
        return try {
            patients.chunked(30).mapIndexed { idx, chunk ->
                tracingService.setTracingEnteredInError(chunk)
                println("Finished chuck ${idx + 1} - size ${chunk.size}")
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun cleanFutureDateMissedAppointment(facilityId: String): Boolean {
        return try {
            tracingService.cleanFutureDateMissedAppointment(facilityId)
            true
        } catch (e: Exception) {
            println(e)
            false
        }
    }
}

interface TracingController {
    fun getStats(id: String): TracingStatsResults

    suspend fun getTracingList(facilityId: String, pagination: PaginationArgs): PaginatedResponse<TracingResult>

    suspend fun setPatientsEnteredInError(patients: List<String>): Boolean
    suspend fun cleanFutureDateMissedAppointment(facilityId: String): Boolean
}