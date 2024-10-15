package org.dtree.fhir.server.services.tracing

import java.time.LocalDate

class TracingStatsResults {
}

data class AppointmentListResults(
    val results: List<Stuff>
)

data class TracingResult(
    val uuid: String,
    val id: String?,
    val name: String,
    val dateAdded: LocalDate?,
    val nextAppointment: LocalDate?,
    val type: List<String>,
    val reasons: List<String>,
    val isFutureAppointment: Boolean?,
    val patientType: String
)

data class TracingListResults(
    val results: List<TracingResult>
)