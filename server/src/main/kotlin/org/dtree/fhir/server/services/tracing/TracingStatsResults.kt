package org.dtree.fhir.server.services.tracing

import java.time.LocalDate

data class TracingStatsResults(
    val total: Int,
    val homeTotal: Int,
    val phoneTotal: Int,
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