package org.dtree.fhir.server.core.search.filters

import org.dtree.fhir.server.core.models.FilterFormItem
import org.dtree.fhir.server.core.util.dateListFromRange
import org.dtree.fhir.server.services.formatDate

typealias FilterFunc = (FilterFormItem) -> List<Pair<String, String>>

val PredefinedFilters = mutableMapOf<String, FilterFunc>(Pair("_tag_location") { filter ->
    val template = "http://smartregister.org/fhir/location-tag|${filter.params[0].value ?: ""}"
    listOf(Pair("_tag", template))
}, Pair("filter-by-date-created") { filter ->
    listOf(Pair("_tag",
        filter.params.first().valueDate?.let { "https://d-tree.org/fhir/created-on-tag|${formatDate(it)}" }
            ?: throw Exception("Date not found")))
}, Pair("filter-by-date-created-range") { filter ->
    dateListFromRange(filter.params.first().valueDateRange).map {
        Pair("_tag", "https://d-tree.org/fhir/created-on-tag|${formatDate(it)}")
    }
})