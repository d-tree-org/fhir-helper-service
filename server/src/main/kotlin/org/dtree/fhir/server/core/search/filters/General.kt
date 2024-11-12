package org.dtree.fhir.server.core.search.filters

import org.dtree.fhir.server.core.models.*
import org.dtree.fhir.server.services.PatientType
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
import java.time.LocalDate

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

fun filterByDate(date: LocalDate) = FilterFormItem(
    filterId = "filter-by-date-created",
    template = "date={date}",
    filterType = FilterTemplateType.template,
    params = listOf(
        FilterFormParamData(
            name = "date", type = FilterParamType.date, valueDate = date
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

fun filterSummary() = FilterFormItem(
    filterId = "_summary",
    template = "_summary={value}",
    filterType = FilterTemplateType.template,
    params = listOf(
        FilterFormParamData(
            name = "value",
            type = FilterParamType.string,
            value = "count"
        )
    )
)

fun filterAddCount(count: Int) = FilterFormItem(
    filterId = "_count",
    template = "_count={value}",
    filterType = FilterTemplateType.template,
    params = listOf(
        FilterFormParamData(
            name = "value",
            type = FilterParamType.string,
            value = count.toString()
        )
    )
)

fun filterRevInclude(custom: String? = null) = FilterFormItem(
    filterId = "_include",
    template = "_include={value}",
    filterType = FilterTemplateType.template,
    params = listOf(
        FilterFormParamData(
            name = "value",
            type = FilterParamType.string,
            value = custom ?: "Appointment:patient"
        )
    )
)

fun addPatientFilter(patients: List<PatientType>, inSubject: Boolean = false): FilterFormItem {
    return FilterFormItem(
        filterId = "patient_filter",
        template = if (inSubject) "subject:Patient._tag=https://d-tree.org/fhir/patient-meta-tag|{value}" else "_tag=https://d-tree.org/fhir/patient-meta-tag|{value}",
        filterType = FilterTemplateType.template,
        params = listOf(
            FilterFormParamData(
                name = "value",
                type = FilterParamType.string,
                value = patients.joinToString(",")
            )
        )
    )
}

fun questionnaireResponseFilters(
    questionnaire: String,
    baseFilters: List<FilterFormItem>,
    hasCount: Boolean = true,
     customParser: ((Bundle) -> Int)? = null
): FilterFormData {
    val filters = mutableListOf(*baseFilters.toTypedArray())
    if (hasCount) {
        filters.add(filterSummary())
    }
    return FilterFormData(
        resource = ResourceType.QuestionnaireResponse.name,
        filterId = "questionnaire-${questionnaire}",
        filters = filters,
        customParser = customParser,
    )
}

fun questionnaireResponseFilter(bundle: Bundle, filterKey: String): Int {
    val items = bundle.entry.map { it.resource as QuestionnaireResponse }.distinctBy { it.subject.reference }
    return items.filter { quest ->
        quest.item.find { it.linkId == filterKey }?.answer?.firstOrNull()?.valueBooleanType?.value ?: false
    }.size
}