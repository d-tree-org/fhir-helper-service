package org.dtree.fhir.server.services

import org.dtree.fhir.server.core.models.FilterFormData
import org.dtree.fhir.server.core.models.FilterFormItem
import org.dtree.fhir.server.core.models.FilterTemplateType
import org.dtree.fhir.server.core.search.filters.PredefinedFilters
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.QuestionnaireResponse
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

enum class PatientType(private val value: String) {
    NEWLY_DIAGNOSED_CLIENT("newly-diagnosed-client"), CLIENT_ALREADY_ON_ART("client-already-on-art"), EXPOSED_INFANT("exposed-infant");

    override fun toString(): String {
        return this.value
    }
}

suspend fun fetchDataTest(data: FilterFormData): ResultData {
  val filters =  data.filters.map { filter ->
        if (filter.filterType == FilterTemplateType.predifined) {
            val filterFunc = PredefinedFilters[filter.template] ?: throw Exception("Template not defined")
            filterFunc(filter)
        } else {
            createFilter(filter)
        }
    }
    println(filters)
    return  ResultData(listOf(), listOf())
}

fun createFilter(filter: FilterFormItem): List<Pair<String, String>> {
    return filter.template.split('&').map {
        val split = it.split("=")
        val tag = split.first()
        var result = split.lastOrNull() ?: ""
        filter.params.forEach { param ->
            val placeholder = "\\{${param.name}}".toRegex()
            result = result.replace(placeholder, param.value ?: "")
        }
        Pair(tag, result)
    }
}

suspend fun fetchData(data: FilterFormData): ResultData {
    println(data.toString())
    var rawDate: List<String>? = null
    val baseFilter = data.filters.map { filter ->
        val temp = mutableMapOf<String, String>()

        when (filter.template) {
            "_tag_location" -> {
                val template = "http://smartregister.org/fhir/location-tag|${filter.params[0].value ?: ""}"
                temp["_tag"] = template
            }

            "date" -> {
                rawDate = filter.params.find { it.name == "date" }?.value?.split("T")?.get(0)?.let { listOf(it) }
            }

            "dateRange" -> {
                val value = filter.params[0].valueDateRange
                if (value != null) {
                    val (from, to) = value
                    if (from != null && to != null) {
                        rawDate = from.datesUntil(to.plusDays(1)).map { it.format(DateTimeFormatter.ISO_LOCAL_DATE) }
                            .toList()
                    } else if (from != null) {
                        rawDate = listOf(
                            from.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        )
                    }
                }
            }

            else -> {
                temp[filter.template] = filter.params[0].value ?: ""
            }
        }

        temp
    }

    rawDate = rawDate?.let { fixDate(it) }

    val bundle = fetchBundle(
        listOf(
            createQuestionnaireResponseFilters("patient-finish-visit", rawDate, baseFilter),
            createPatientFilters(listOf(PatientType.NEWLY_DIAGNOSED_CLIENT), rawDate, baseFilter),
            createPatientFilters(listOf(PatientType.CLIENT_ALREADY_ON_ART), rawDate, baseFilter),
            createPatientFilters(listOf(PatientType.EXPOSED_INFANT), rawDate, baseFilter),
            createQuestionnaireResponseFilters("exposed-infant-milestone-hiv-test", rawDate, baseFilter, false),
            createQuestionnaireResponseFilters("art-client-viral-load-collection", rawDate, baseFilter, false),
            createPatientFilters(
                listOf(PatientType.NEWLY_DIAGNOSED_CLIENT), null, baseFilter, options = Options(hasCount = true)
            ),
            createPatientFilters(
                listOf(PatientType.CLIENT_ALREADY_ON_ART), null, baseFilter, options = Options(hasCount = true)
            ),
            createPatientFilters(
                listOf(PatientType.EXPOSED_INFANT), null, baseFilter, options = Options(hasCount = true)
            ),
            createQuestionnaireResponseFilters(
                "patient-finish-visit",
                rawDate,
                baseFilter,
                true,
                listOf(mapOf("subject:Patient._tag" to "https://d-tree.org/fhir/patient-meta-tag|newly-diagnosed-client"))
            ),
            createQuestionnaireResponseFilters(
                "patient-finish-visit",
                rawDate,
                baseFilter,
                true,
                listOf(mapOf("subject:Patient._tag" to "https://d-tree.org/fhir/patient-meta-tag|client-already-on-art"))
            ),
            createQuestionnaireResponseFilters(
                "patient-finish-visit",
                rawDate,
                baseFilter,
                true,
                listOf(mapOf("subject:Patient._tag" to "https://d-tree.org/fhir/patient-meta-tag|exposed-infant"))
            )
        )
    )

    val summary = listOf(
        "Total visits",
        "Newly diagnosed clients (new)",
        "Already on Art (new)",
        "Exposed infant (new)",
        "Milestone answered",
        "VL collected answered",
        "Newly diagnosed clients (all)",
        "Already on Art (all)",
        "Exposed infant (all)",
        "Newly diagnosed clients (visits)",
        "Already on Art (visits)",
        "Exposed infant (visits)"
    )

    return ResultData(summaries = getResults(bundle, summary, listOf(Filter(4) { resource ->
        resource?.item?.firstOrNull()?.item?.find { it.linkId == "able-to-conduct-test" }?.answer?.firstOrNull()?.valueBooleanType?.value
            ?: false
    }, Filter(5) { resource ->
        resource?.item?.find { it.linkId == "viral-load-collection-confirmation" }?.answer?.firstOrNull()?.valueBooleanType?.value
            ?: false
    })), date = rawDate)
}

fun getResults(
    bundle: Bundle?, summary: List<String>, filters: List<Filter>
): List<SummaryItem> {
    if (bundle == null) return emptyList()

    return bundle.entry?.mapIndexed { idx, entry ->
        val filter = filters.find { it.index == idx }?.filter
        if (filter != null) {
            val items = (entry.resource as? Bundle)?.entry ?: emptyList()
            val unique = items.distinctBy { (it.resource as? QuestionnaireResponse)?.subject?.reference }
                .mapNotNull { if (it.resource is QuestionnaireResponse) it.resource as QuestionnaireResponse else null }
            SummaryItem(
                name = summary[idx], value = unique.filter(filter).size
            )
        } else {
            SummaryItem(
                name = summary[idx], value = (entry.resource as? Bundle)?.total ?: 0
            )
        }
    } ?: emptyList()
}

data class ResultData(val summaries: List<SummaryItem>, val date: List<String>?)
data class SummaryItem(val name: String, val value: Int)

data class Filter(val index: Int, val filter: (QuestionnaireResponse?) -> Boolean)

suspend fun fetchBundle(filters: List<String>): Bundle? {
    println(filters)
    return null
}

fun fixDate(date: List<String>): List<String> {
    // Implement your date fixing logic here
    return date
}

fun createQuestionnaireResponseFilters(
    questionnaire: String,
    date: List<String>?,
    baseFilter: List<Map<String, String>>,
    hasCount: Boolean = true,
    extras: List<Map<String, String>> = emptyList()
): String {
    val query = QueryParam(mapOf("questionnaire" to questionnaire))

    if (hasCount) {
        query.set("_summary", "count")
    }
    query.fromArray(baseFilter)
    query.remove("date")
    query.remove("dateRange")

    if (date != null) {
//        query.add("_tag", date.joinToString(",") { "https://d-tree.org/fhir/created-on-tag|${formatDate(it)}" })
    }

    query.fromArray(extras)
    return query.toUrl("/QuestionnaireResponse")
}

fun createPatientFilters(
    types: List<PatientType>? = null,
    date: List<String>?,
    baseFilter: List<Map<String, String>>,
    options: Options = Options()
): String {
    val query = QueryParam()

    if (options.hasCount) {
        query.add("_summary", "count")
    }

    query.fromArray(baseFilter)
    query.remove("date")
    query.remove("dateRange")

    if (date != null) {
//        query.add("_tag", date.joinToString(",") { "https://d-tree.org/fhir/created-on-tag|${formatDate(it)}" })
    }

    if (types != null) {
        query.add("_tag", "https://d-tree.org/fhir/patient-meta-tag|${types.joinToString(",")}")
    }

    return query.toUrl("/Patient")
}

fun formatDate(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}

data class Options(val hasCount: Boolean = true, val onlyActive: Boolean = false, val formatUrl: Boolean = false)

class QueryParam(
    values: Map<String, String> = mapOf(), private val encodeUrl: Boolean = false
) {
    private val queries: MutableMap<String, String> = mutableMapOf()

    init {
        from(values)
    }

    fun add(key: String, value: Any) {
        if (queries.containsKey(key)) {
            queries["$key[${Math.random()}]"] = value.toString()
        } else {
            queries[key] = value.toString()
        }
    }

    fun get(key: String): String? {
        return queries[key]
    }

    fun set(key: String, value: Any) {
        queries[key] = value.toString()
    }

    fun remove(key: String) {
        queries.remove(key)
    }

    private fun from(values: Map<String, String>) {
        for ((key, value) in values) {
            add(key, value)
        }
    }

    fun has(key: String): Boolean {
        return queries.containsKey(key)
    }

    fun fromArray(values: List<Map<String, String>>) {
        for (valueMap in values) {
            from(valueMap)
        }
    }

    fun toUrl(resources: String): String {
        val query = queries.map { (key, value) ->
            if (key.contains("[")) {
                "${key.split("[")[0]}=${if (encodeUrl) value.encodeUrl() else value}"
            } else {
                "$key=${if (encodeUrl) value.encodeUrl() else value}"
            }
        }.joinToString("&")
        return "$resources?$query"
    }

    private fun String.encodeUrl(): String {
        return java.net.URLEncoder.encode(this, "UTF-8")
    }
}
