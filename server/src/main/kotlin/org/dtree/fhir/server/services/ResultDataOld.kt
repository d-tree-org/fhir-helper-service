package org.dtree.fhir.server.services

import java.time.LocalDate
import java.time.LocalDateTime

data class FacilityResultData(val groups: List<GroupedSummaryItem>, val date: LocalDate, val generatedDate: LocalDateTime)

data class GroupedSummaryItem(
    val groupKey: String,
    val groupTitle: String,
    val summaries: List<SummaryItem>,
    val order: Int,
)

data class SummaryItem(val name: String, val value: Int)


data class ResultDataOld(val summaries: List<SummaryItem>, val date: List<String>?)