package org.dtree.fhir.server.services

import java.time.LocalDate

data class ResultDataOld(val summaries: List<SummaryItem>, val date: List<String>?)

data class ResultData(val summaries: Map<String, List<SummaryItem>>, val date: LocalDate)

data class SummaryItem(val name: String, val value: Int)