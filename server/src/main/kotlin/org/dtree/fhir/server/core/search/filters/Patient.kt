package org.dtree.fhir.server.core.search.filters

import org.dtree.fhir.server.core.models.FilterFormItem
import org.dtree.fhir.server.core.models.FilterFormParamData
import org.dtree.fhir.server.core.models.FilterParamType

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