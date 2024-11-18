package org.dtree.fhir.server.services.tracing

import ca.uhn.fhir.rest.gclient.TokenClientParam
import org.dtree.fhir.core.uploader.general.FhirClient
import org.dtree.fhir.core.uploader.general.paginateExecute
import org.dtree.fhir.core.utilities.ReasonConstants
import org.dtree.fhir.core.utilities.SystemConstants
import org.dtree.fhir.core.utils.extractOfficialIdentifier
import org.dtree.fhir.core.utils.logicalId
import org.dtree.fhir.server.core.cache.CacheManager
import org.dtree.fhir.server.core.cache.PaginationUtil
import org.dtree.fhir.server.core.models.FilterFormData
import org.dtree.fhir.server.core.models.FilterTemplateType
import org.dtree.fhir.server.core.models.PaginatedResponse
import org.dtree.fhir.server.core.models.PaginationArgs
import org.dtree.fhir.server.core.search.filters.PredefinedFilters
import org.dtree.fhir.server.core.search.filters.filterAddCount
import org.dtree.fhir.server.core.search.filters.filterRevInclude
import org.dtree.fhir.server.core.search.filters.tracingFiltersByFacility
import org.dtree.fhir.server.services.QueryParam
import org.dtree.fhir.server.services.createFilter
import org.hl7.fhir.r4.model.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.ZoneId

object TracingService : KoinComponent {
    private val client by inject<FhirClient>()
    private val cacheManager = CacheManager<TracingResult>()
    val paginationUtil = PaginationUtil<TracingResult>()

    fun getStats(facilityId: String): TracingStatsResults {
        val filter = FilterFormData(
            resource = ResourceType.Task.name,
            filterId = "random_filter",
            filters = listOf(
                filterAddCount(20000),
            ) + tracingFiltersByFacility(facilityId)
        )

        val results =
            fetch(
                client = client,
                actions = listOf(filter),
                hasIncludes = false,
                encode = true
            ).associateBy(keySelector = {
                (it.main as Task).`for`.reference.split("/").last()
            }, valueTransform = {
                (it.main as Task).meta.tag.firstOrNull { tag -> tag.system == SystemConstants.CONTACT_TRACING_SYSTEM }?.code
            }).values.mapNotNull { it }
        val homeTotal = results.filter { ReasonConstants.homeTracingCoding.code == it }.size
        val total = results.size
        return TracingStatsResults(
            total = total,
            homeTotal = homeTotal,
            phoneTotal = total - homeTotal
        )
    }

    suspend fun getTracingList(facilityId: String, pagination: PaginationArgs): PaginatedResponse<TracingResult> {
        if (pagination.all) {
            val data = getTracingListFromServer(facilityId)
            return PaginatedResponse(
                items = data,
                page = 0,
                pageSize = 0,
                totalItems = data.size,
                totalPages = 0,
            )
        }
        var data = cacheManager.get(facilityId)
        if (data == null) {
            data = getTracingListFromServer(facilityId)
            cacheManager.set(facilityId, data)
        }

        val paginatedResponse = paginationUtil.paginate(data, pagination.pageSize, pagination.page)
        return paginatedResponse
    }

    private suspend fun getTracingListFromServer(facilityId: String): List<TracingResult> {
        val filter = FilterFormData(
            resource = ResourceType.Task.name,
            filterId = "random_filter",
            filters = listOf(
                filterAddCount(20000),
                filterRevInclude("Task:patient"),
            ) + tracingFiltersByFacility(facilityId)
        )

        val results = fetch(client = client, actions = listOf(filter), hasIncludes = true, encode = true)
        val map = mutableMapOf<String, Temp>()
        results.forEach { result ->
            val patient = result.include as Patient
            if (map.contains(patient.logicalId)) {
                val item = map[patient.logicalId]!!
                map[patient.logicalId] = (item.copy(tasks = item.tasks + listOf(result.main as Task)))
            } else {
                map[patient.logicalId] = Temp(listOf(result.main as Task), patient)
            }
        }
        val allPatientsToFetch = map.keys
        val appointmentMap = mutableMapOf<String, Appointment>()
        val appointmentBundle = client.fetchResourcesFromList(allPatientsToFetch.toList())
        appointmentBundle.entry.forEach { entry ->
            val appointment = entry.resource as Appointment
            if (appointment.hasStart() && appointment.hasParticipant() && (appointment.status == Appointment.AppointmentStatus.BOOKED ||
                        appointment.status == Appointment.AppointmentStatus.WAITLIST ||
                        appointment.status == Appointment.AppointmentStatus.NOSHOW)
            ) {
                val patient =
                    appointment.participant.first { it.actor.reference.contains("Patient") }.actor.reference.split("/")
                        .last()
                appointmentMap[patient] = appointment
            }
        }
        return map.map { entry ->
            val mPatient = entry.value.patient
            val type = mutableSetOf<String>()
            var mDate: LocalDate? = null
            val reasons = entry.value.tasks.map { task ->
                println(task.logicalId)
                task.meta.tag.firstOrNull { tag -> tag.system == SystemConstants.CONTACT_TRACING_SYSTEM }?.code?.let {
                    type.add(it)
                }
                mDate = task.authoredOn?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
                task.reasonCode.text
            }
            val appointmentDate = appointmentMap[mPatient.logicalId]?.start?.toInstant()?.atZone(ZoneId.systemDefault())
                ?.toLocalDate()
            val patientTypes =
                mPatient.meta.tag
                    .filter { it.system == SystemConstants.PATIENT_TYPE_FILTER_TAG_VIA_META_CODINGS_SYSTEM }
                    .map { it.code }
            val patientType: String = SystemConstants.getCodeByPriority(patientTypes) ?: patientTypes.first()
            TracingResult(
                uuid = mPatient.logicalId,
                id = mPatient.extractOfficialIdentifier(),
                name = mPatient.nameFirstRep.nameAsSingleString,
                dateAdded = mDate,
                type = type.toList(),
                patientType = patientType,
                reasons = reasons,
                nextAppointment = appointmentDate,
                isFutureAppointment = appointmentDate?.isAfter(LocalDate.now())
            )
        }.distinctBy { it.uuid }
    }

    suspend fun setTracingEnteredInError(patientId: List<String>) {
        val tasks: List<Task> = client.fhirClient.search<Bundle>().forResource(Task::class.java)
            .where(Task.PATIENT.hasAnyOfIds(*patientId.toTypedArray()))
            .where(
                TokenClientParam("_tag").exactly()
                    .codings(ReasonConstants.homeTracingCoding, ReasonConstants.phoneTracingCoding)
            )
            .where(
                Task.STATUS.exactly().codes(
                    Task.TaskStatus.READY.toCode(),
                    Task.TaskStatus.INPROGRESS.toCode(),
                    Task.TaskStatus.REQUESTED.toCode(),
                    Task.TaskStatus.ACCEPTED.toCode(),
                    Task.TaskStatus.ONHOLD.toCode()
                )
            )
            .paginateExecute<Task>(client).mapNotNull { task ->
                if (task.status == Task.TaskStatus.COMPLETED || task.status == Task.TaskStatus.CANCELLED || task.status == Task.TaskStatus.ENTEREDINERROR) {
                    return@mapNotNull null
                }
                task.meta.addTag(
                    ReasonConstants.resourceEnteredInError
                )
                task.status = Task.TaskStatus.ENTEREDINERROR
                task
            }
        println("Tasks entered in error ${tasks.size}")
        if (tasks.isEmpty()) {
            println("No Tracing tasks")
            return
        }
        client.bundleUpload(tasks, 30)
    }

    suspend fun cleanFutureDateMissedAppointment(facilityId: String) {
        val results = getTracingList(facilityId, PaginationArgs(all = true)).items.mapNotNull {
            if (it.isFutureAppointment == true) it.uuid
            else null
        }
        if (results.isEmpty()) return
        setTracingEnteredInError(results)
    }
}

data class Temp(val tasks: List<Task>, val patient: Patient)

fun fetch(
    client: FhirClient,
    actions: List<FilterFormData>,
    hasIncludes: Boolean = true,
    encode: Boolean = false
): MutableList<ResultClass> {
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
        val query = QueryParam(
            encodeUrl = encode
        )
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
    resultBundle.entry.forEachIndexed { _, entry ->
        val resource = entry.resource
        if (resource is Bundle) {
            println(resource.entry.size)
            if (hasIncludes) results.addAll(handleIncludes(resource)) else results.addAll(resource.entry.map {
                ResultClass(
                    main = it.resource
                )
            })
        }
    }
    return results
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
            } else if (resource is Task) {
                val patient = resource.`for`.reference.split("/").last()
                includes[patient] = resource.logicalId
            }
        }
    }
    return final
}

data class ResultClass(val main: Resource, val include: Resource? = null)