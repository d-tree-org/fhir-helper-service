package org.dtree.fhir.server.plugins.tasks

import java.util.*

data class ChangeAppointmentData(
    val id: String,
    val date: Date,
)

data class TracingEnteredErrorData(
    val type: TracingRemovalType,
    val data: List<String>,
)

data class ChangeStatusData(
    val type: ChangeStatusType,
    val data: List<String>,
)

enum class TracingRemovalType {
    EnteredInError,
    TransferredOut,
    Deceased
}

enum class ChangeStatusType {
    Discharged,
    Deceased,
    EnteredInError
}