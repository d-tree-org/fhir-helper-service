package org.dtree.fhir.server.plugins.tasks

import java.util.*

data class ChangeAppointmentData(
    val id: String,
    val date: Date,
)
