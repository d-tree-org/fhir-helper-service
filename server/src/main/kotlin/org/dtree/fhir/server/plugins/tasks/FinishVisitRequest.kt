package org.dtree.fhir.server.plugins.tasks

import java.util.*

data class FinishVisitRequest(
    val id: String,
    val date: Date,
    val dateVisited: Date? = null,
)