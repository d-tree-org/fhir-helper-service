package org.dtree.fhir.server.services

import java.time.LocalDate

data class FacilityResultData(val groups: List<GroupedSummaryItem>, val date: LocalDate)

data class GroupedSummaryItem(
    val groupKey: String,
    val groupTitle: String,
    val summaries: List<SummaryItem>
)

data class SummaryItem(val name: String, val value: Int)


data class ResultDataOld(val summaries: List<SummaryItem>, val date: List<String>?)