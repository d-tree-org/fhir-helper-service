package org.dtree.fhir.server.services

import org.dtree.fhir.core.uploader.general.FhirClient
import org.dtree.fhir.server.core.models.FilterFormData
import org.dtree.fhir.server.core.models.FilterFormItem
import org.dtree.fhir.server.core.models.FilterTemplateType
import org.dtree.fhir.server.core.search.filters.PredefinedFilters
import org.dtree.fhir.server.services.stats.*
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.QuestionnaireResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.collections.set
import kotlin.random.Random

enum class PatientType(val value: String) {
    NEWLY_DIAGNOSED_CLIENT("newly-diagnosed-client"), CLIENT_ALREADY_ON_ART("client-already-on-art"), EXPOSED_INFANT("exposed-infant");

    override fun toString(): String {
        return this.value
    }
}

suspend fun fetchDataTest(client: FhirClient, actions: List<FilterFormData>): FacilityResultData {
    val requests = mutableListOf<Bundle.BundleEntryRequestComponent>()
    for (data in actions) {
        val filters = data.filters.map { filter ->
            if (filter.filterType == FilterTemplateType.predifined) {
                val filterFunc = PredefinedFilters[filter.template] ?: throw Exception("Template not defined")
                filterFunc(filter)
            } else {
                createFilter(filter)
            }
        }
        println(filters)
        val query = QueryParam()
        for (filter in filters) {
            filter.forEach { query.set(it.first, it.second) }
        }
        val requestUrl = query.toUrl("/${data.resource}")
        println(requestUrl)
        requests.add(Bundle.BundleEntryRequestComponent().apply {
            method = Bundle.HTTPVerb.GET
            url = requestUrl
            id = data.filterId
        })
    }
    val resultBundle: Bundle = client.fetchBundle(requests)
    val defaultGroupId = Random.nextInt().toString()
    val summaries = mutableMapOf<String, ArrayList<SummaryItem>>()
    resultBundle.entry.forEachIndexed { idx, entry ->
        val filter = actions[idx]

        val value = if (filter.customParser == null) {
            (entry.resource as Bundle).total
        } else {
            filter.customParser.invoke((entry.resource as Bundle))
                ?: throw Exception("Pass a custom parser " + filter.filterId)
        }
        val groupKey = filter.groupId ?: defaultGroupId
        val list = summaries.getOrPut(groupKey) { ArrayList() }
        list.add(
            SummaryItem(
                name = filter.title ?: "", value = value,
            )
        )
    }

    return FacilityResultData(
        groups = summaries.map {
            val group = mapKeyToTitle(it.key)
            GroupedSummaryItem(
                groupKey = it.key,
                groupTitle = group.title,
                summaries = it.value,
                order = group.order,
                startCollapsed = group.startCollapsed,
            )
        },
        date = LocalDate.now(), generatedDate = LocalDateTime.now(),
    )
}

fun mapKeyToTitle(key: String): GroupMeta {
    return when (key) {
        "visits" -> GroupMeta("Today's visits", 1)
        "tasks" -> GroupMeta("Today's Tasks", 2)
        "newPatients" -> GroupMeta("New clients today", 3)
        else -> GroupMeta("Facility Patient totals", 0, true)
    }
}

fun createFilter(filter: FilterFormItem): List<Pair<String, String>> {
    return filter.template.split('&').map {
        val split = it.split("=")
        val tag = split.first()
        var result = split.lastOrNull() ?: ""
        filter.params.forEach { param ->
            val placeholder = "\\{${param.name}}".toRegex()
            result = result.replace(placeholder, param.valueString() ?: "")
        }
        Pair(tag, result)
    }
}

fun formatDate(date: LocalDate, dashed: Boolean = false): String {
    return date.format(DateTimeFormatter.ofPattern(if(dashed) "yyyy-MM-dd" else "dd/MM/yyyy"))
}