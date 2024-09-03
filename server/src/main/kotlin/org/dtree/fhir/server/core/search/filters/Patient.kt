package org.dtree.fhir.server.core.search.filters

import org.dtree.fhir.server.core.models.FilterFormData
import org.dtree.fhir.server.core.models.FilterFormItem
import org.dtree.fhir.server.core.models.FilterFormParamData
import org.dtree.fhir.server.core.models.FilterParamType
import org.dtree.fhir.server.services.PatientType
import org.hl7.fhir.r4.model.ResourceType

fun givenNameFilter(name: String) = FilterFormItem(
    filterId = "filter-by-name",
    template = "given={name}",
    params = listOf(
        FilterFormParamData(
            name = "name",
            type = FilterParamType.string,
            value = name
        )
    )
)

fun patientTypeFilter(patients: List<PatientType>, baseFilters: List<FilterFormItem>, hasCount: Boolean = true): FilterFormData {
    val filters = mutableListOf(addPatientFilter(patients), *baseFilters.toTypedArray())
    if (hasCount) {
        filters.add(filterSummary())
    }
    return FilterFormData(
        resource = ResourceType.Patient.name,
        filterId = "patients-${patients.joinToString(",")}",
        filters = filters
    )
}