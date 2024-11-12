package org.dtree.fhir.server.services.appointment

import java.time.LocalDate

data class AppointmentListResults(
    val results: List<AppointmentResultItem>
)

data class AppointmentResultItem(val uuid: String, val id: String?, val name: String, val date: LocalDate?)