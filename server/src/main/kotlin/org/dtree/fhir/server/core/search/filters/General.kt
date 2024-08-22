package org.dtree.fhir.server.core.search.filters

import org.dtree.fhir.server.core.models.*
import java.time.LocalDate
import java.util.*

fun filterByLocation(location: String) = filterByPredefined(
    id = "filter-by-location", template = "_tag_location",
    params = listOf(
        FilterFormParamData(
            name = "location", type = FilterParamType.string, value = location
        )
    ),
)

fun filterByDateCreated(dateCreated: LocalDate) = filterByPredefined(
    id = "filter-by-date-created",
    template = "filter-by-date-created",
    params = listOf(
        FilterFormParamData(
            name = "date", type = FilterParamType.date, valueDate = dateCreated
        )
    ),
)

fun filterByDateCreatedRange(date: DateRange) = filterByPredefined(
    id = "filter-by-date-created-range",
    template = "filter-by-date-created-range",
    params = listOf(
        FilterFormParamData(
            name = "dateRange", type = FilterParamType.dateRange, valueDateRange = date
        )
    ),
)

private fun filterByPredefined(id: String, template: String, params: List<FilterFormParamData>) = FilterFormItem(
    filterId = id, template = template, filterType = FilterTemplateType.predifined, params = params
)