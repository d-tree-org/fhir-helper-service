package org.dtree.fhir.server.core.search.filters

import org.dtree.fhir.server.core.models.FilterFormItem
import org.dtree.fhir.server.core.models.FilterFormParamData
import org.dtree.fhir.server.core.models.FilterParamType
import org.dtree.fhir.server.core.models.FilterTemplateType

 fun tracingFiltersByFacility(facilityId: String): List<FilterFormItem> {
    val locationFilter = filterByLocation(facilityId)
    val filterByActive = FilterFormItem(
        filterId = "filter-by-task-status",
        template = "status={status}",
        filterType = FilterTemplateType.template,
        params = listOf(
            FilterFormParamData(
                name = "status",
                type = FilterParamType.string,
                value = listOf("ready", "in-progress").joinToString(",")
            )
        ),
    )
    val filterTracingTask = FilterFormItem(
        filterId = "filter-by-task-tracing-code",
        template = "code={code}",
        filterType = FilterTemplateType.template,
        params = listOf(
            FilterFormParamData(
                name = "code",
                type = FilterParamType.string,
                value = "225368008"
            )
        ),
    )
    return listOf( locationFilter,
        filterByActive,
        filterTracingTask)
}
