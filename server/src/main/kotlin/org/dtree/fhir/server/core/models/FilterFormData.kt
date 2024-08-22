package org.dtree.fhir.server.core.models

import java.util.*

data class FilterFormData(
    val filters: List<FilterFormItem>
)

data class FilterFormItem(
    val filterId: String,
    val template: String,
    val params: List<FilterFormParamData>,
    val filterType: FilterTemplateType = FilterTemplateType.template
)

data class FilterFormParamData(
    val name: String,
    val type: FilterParamType,
    val value: String? = null,
    val valueDate: Date? = null,
    val valueDateRange: DateRange? = null
)

data class DateRange(
    val from: Date?,
    val to: Date? = null
)

enum class FilterParamType {
    string, date, dateRange, number, boolean, select
}

enum class FilterTemplateType {
    predifined, template
}


fun filterItem(id: String, init: FilterFormItem.() -> Unit): FilterFormItem {
    val filter = FilterFormItem(
        filterId = id,
        template = "given={name}",
        params = listOf()
    )
    filter.init()
    return filter
}

fun FilterFormItem.addParamData(data: FilterFormParamData, init: FilterFormItem.() -> Unit): FilterFormItem {
    val filter = this.copy(
        params = this.params + listOf(data)
    )
    filter.init()
    return filter
}