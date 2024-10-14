package org.dtree.fhir.server.services.tracing

import org.dtree.fhir.core.uploader.general.FhirClient
import org.dtree.fhir.core.utils.logicalId
import org.dtree.fhir.server.core.models.FilterFormData
import org.dtree.fhir.server.core.models.FilterTemplateType
import org.dtree.fhir.server.core.search.filters.*
import org.dtree.fhir.server.services.QueryParam
import org.dtree.fhir.server.services.createFilter
import org.dtree.fhir.server.util.extractOfficialIdentifier
import org.hl7.fhir.r4.model.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.ZoneId

object TracingService : KoinComponent {
    private val client by inject<FhirClient>()
    fun getStats(id: String): TracingStatsResults {
        TODO("Not yet implemented")
    }

    fun getAppointmentList(facilityId: String, date: LocalDate): AppointmentListResults {
        val dateFilter = filterByDate(date)
        val locationFilter = filterByLocation(facilityId)
        val filter = FilterFormData(
            resource = ResourceType.Appointment.name,
            filterId = "random_filter",
            filters = listOf(dateFilter, filterAddCount(20000), filterRevInclude(), locationFilter)
        )

        return fetch(client, listOf(filter))
    }
}

fun fetch(client: FhirClient, actions: List<FilterFormData>): AppointmentListResults {
    val requests = mutableListOf<Bundle.BundleEntryRequestComponent>()
    val results = mutableListOf<ResultClass>()
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
    resultBundle.entry.forEachIndexed { idx, entry ->
        val filter = actions[idx]
        val resource = entry.resource
        if (resource is Bundle) {
            println(resource.entry.size)
            results.addAll(handleIncludes(resource))
        }
    }
    return AppointmentListResults(results.map {
        var patient = (it.include as Patient)
        val appointment = it.main as Appointment
        val date = appointment.start?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
        Stuff(patient.nameFirstRep.nameAsSingleString, patient.extractOfficialIdentifier(), date)
    })
}

fun handleIncludes(bundle: Bundle): List<ResultClass> {
    val final = mutableListOf<ResultClass>()
    val main = mutableMapOf<String, Resource>()
    val includes = mutableMapOf<String, String>()

    for (entry in bundle.entry) {
        val resource = entry.resource
        if (entry.search.mode == Bundle.SearchEntryMode.INCLUDE) {
            val id = resource.logicalId
            val map = includes[id]
            final.add(ResultClass(main[map]!!, entry.resource!!))
        } else {
            main[entry.resource.logicalId] = resource
            if (resource is Appointment) {
                val patient =
                    resource.participant.first { it.actor.reference.contains("Patient") }.actor.reference.split("/")
                        .last()
                includes[patient] = resource.logicalId
            }
        }
    }
    return final
}

data class Stuff(val name: String, val id: String?, val date: LocalDate?)

data class ResultClass(val main: Resource, val include: Resource)