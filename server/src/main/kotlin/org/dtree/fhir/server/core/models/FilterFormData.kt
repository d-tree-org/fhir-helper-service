package org.dtree.fhir.server.core.models

import org.dtree.fhir.server.services.formatDate
import org.hl7.fhir.r4.model.Bundle
import java.time.LocalDate

data class FilterFormData(
    val resource: String,
    val filterId: String,
    val filters: List<FilterFormItem>,
    val title: String? = null,
    val groupId: String? = null,
    val customParser: ((Bundle) -> Int)? = null
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
    val valueDate: LocalDate? = null,
    val valueDateRange: DateRange? = null,
) {
    fun valueString(): String? {
        if (type == FilterParamType.string) {
            return value
        } else if (type == FilterParamType.date) {
            return valueDate?.let { formatDate(it, true) }
        } else {
            return valueDateRange?.toString()
        }
    }
}

data class DateRange(
    val from: LocalDate?,
    val to: LocalDate? = null
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
        params = listOf(),
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