package org.dtree.fhir.core.uploader.general

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.okhttp.client.OkHttpRestfulClientFactory
import ca.uhn.fhir.parser.IParser
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.gclient.IQuery
import ca.uhn.fhir.util.BundleUtil
import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.dtree.fhir.core.models.PatientData
import org.dtree.fhir.core.models.parsePatientResources
import org.dtree.fhir.core.utils.Logger
import org.dtree.fhir.core.utils.logicalId
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Appointment
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import java.util.concurrent.TimeUnit


class FhirClient(private val dotenv: Dotenv, private val iParser: IParser) {
    val fhirClient: IGenericClient
    private val okHttpClient: OkHttpClient
    val ctx: FhirContext = FhirContext.forR4()

    init {
        okHttpClient = createOkHttpClient()
        val factory = OkHttpRestfulClientFactory()
        factory.fhirContext = ctx
        factory.setHttpClient(okHttpClient)
        ctx.restfulClientFactory = factory
        fhirClient = ctx.newRestfulGenericClient(dotenv["FHIR_BASE_URL"])
    }

    private fun createOkHttpClient(): OkHttpClient {
        val tokenAuthenticator = TokenAuthenticator.createAuthenticator(dotenv)
        val authInterceptor = AuthInterceptor(tokenAuthenticator)
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC)

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(authInterceptor)
            .readTimeout(2, TimeUnit.MINUTES)
            .build()
    }

    suspend inline fun <reified T : Resource> searchResources(
        count: Int = 100,
        limit: Int? = null,
        noinline search: IQuery<IBaseBundle>.() -> Unit
    ): List<T> {
        val resources: MutableList<IBaseResource> = mutableListOf()
        val query =
            fhirClient.search<IBaseBundle>().forResource(T::class.java).apply(search).returnBundle(Bundle::class.java)
                .count(count)
        if (limit != null) {
            query.count(limit)
        }
        var bundle = query.execute()
        resources.addAll(BundleUtil.toListOfResources(ctx, bundle))

        if (limit == null) {
            while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
                Logger.info(bundle.link.map { it.url }.toString())
                bundle = fhirClient.loadPage().next(bundle).execute()
                resources.addAll(BundleUtil.toListOfResources(ctx, bundle))
            }
        }
        return resources.toList() as List<T>
    }

    suspend inline fun <reified T : Resource> transaction(requests: List<Bundle.BundleEntryRequestComponent>): List<T> {
        val bundle = Bundle()
        bundle.setType(Bundle.BundleType.TRANSACTION)
        bundle.entry.addAll(requests.map { rq ->
            Bundle.BundleEntryComponent().apply {
                request = rq
            }
        })
        val resBundle = fhirClient.transaction().withBundle(bundle).execute()
        val resources: MutableList<IBaseResource> = mutableListOf()
        resources.addAll(BundleUtil.toListOfResources(ctx, resBundle))
        return resources.toList() as List<T>
    }

    fun fetchBundle(list: List<Bundle.BundleEntryRequestComponent>): Bundle {
        val bundle = Bundle()
        bundle.setType(Bundle.BundleType.BATCH)
        bundle.entry.addAll(
            list.map { rq ->
                Bundle.BundleEntryComponent().apply {
                    request = rq
                    fullUrl = rq.url

                }
            }
        )
        return fhirClient.transaction().withBundle(bundle).execute()
    }

    fun fetchResourcesFromList(ids: List<String>): Bundle {
        Appointment.AppointmentStatus.WAITLIST
        val item = Bundle.BundleEntryRequestComponent().apply {
            method = Bundle.HTTPVerb.GET
            url = "Appointment?patient=${ids.joinToString(",")}&status=${
                listOf(
                    "waitlist",
                    "booked",
                    "noshow"
                ).joinToString(",")
            }&_count=20000"
            id = "filter"
        }
        return fetchBundle(listOf(item)).entry.first().resource as Bundle
    }

    fun fetchResourcesFromList(type: ResourceType, ids: List<String>): Bundle {
        val item = Bundle.BundleEntryRequestComponent().apply {
            method = Bundle.HTTPVerb.GET
            url = "${type.name}?_id=${ids.joinToString(",")}"
            id = "filter"
        }
        return fetchBundle(listOf(item)).entry.first().resource as Bundle
    }

    /**
     * Given a patient id fetch the following active items
     * - Patient
     * - Guardians (Patient/RelatedPerson)
     * - Observations
     * - Practitioner
     * - Current CarePlan
     * - Tasks
     * - Conditions
     * - Observations
     * - Appointment
     * - List
     */
    fun fetchAllPatientsActiveItems(patientId: String): PatientData {
        val bundle = Bundle()
        bundle.type = Bundle.BundleType.BATCH

        bundle.addEntry()
            .request
            .setMethod(Bundle.HTTPVerb.GET)
            .setUrl("Patient?_id=$patientId&_include=Patient:link&_include=Patient:general-practitioner")

        bundle.addEntry()
            .request
            .setMethod(Bundle.HTTPVerb.GET)
            .setUrl("Observation?subject=$patientId&status=preliminary")

        bundle.addEntry()
            .request
            .setMethod(Bundle.HTTPVerb.GET)
            .setUrl("CarePlan?subject=$patientId&_count=1&status=completed&_sort=-_lastUpdated")

        bundle.addEntry()
            .request
            .setMethod(Bundle.HTTPVerb.GET)
            .setUrl("CarePlan?subject=$patientId&status=active,on-hold")

        bundle.addEntry()
            .request
            .setMethod(Bundle.HTTPVerb.GET)
            .setUrl("Task?patient=$patientId&status=ready,in-progress,on-hold&_count=100000")

        bundle.addEntry()
            .request
            .setMethod(Bundle.HTTPVerb.GET)
            .setUrl("Condition?subject=$patientId&clinical-status=active")

        bundle.addEntry()
            .request
            .setMethod(Bundle.HTTPVerb.GET)
            .setUrl("Appointment?actor=$patientId&status=waitlist,booked,noshow")

        bundle.addEntry()
            .request
            .setMethod(Bundle.HTTPVerb.GET)
            .setUrl("List?subject=$patientId&status=current")

        println(iParser.encodeResourceToString(bundle))
        return fhirClient.transaction()
            .withBundle(bundle)
            .prettyPrint()
            .execute().parsePatientResources(patientId)
    }

    suspend fun bundleUpload(
        list: List<Bundle.BundleEntryComponent>,
        batchSize: Int
    ) {
        val totalBatches = if (list.size % batchSize == 0) list.size / batchSize else list.size / batchSize + 1

        for (batchIndex in 0..<totalBatches) {
            val start = batchIndex * batchSize
            val end = minOf((batchIndex + 1) * batchSize, list.size)
            val batchFiles = list.subList(start, end)

            val response = uploadBatchUpload(batchFiles)

            if (response is DataResponseState.Success) {
                Logger.info("Uploaded successfully")
            } else if (response is DataResponseState.Error) {
                throw Exception(response.exception)
            }
        }
    }

    @JvmName("bundleUploadResource")
    suspend fun bundleUpload(
        list: List<Resource>,
        batchSize: Int
    ) {
        bundleUpload(list.map { res ->
            Bundle.BundleEntryComponent().apply {
                resource = res.apply {
                    id = logicalId
                }
                request = Bundle.BundleEntryRequestComponent().apply {
                    if (res.hasId()) {
                        method = Bundle.HTTPVerb.PUT
                        url = "${res.resourceType.name}/${res.logicalId}"
                    }
                    else {
                        method = Bundle.HTTPVerb.POST
                        url = res.resourceType.name
                    }
                }
            }
        }, batchSize)
    }

    private suspend fun uploadBatchUpload(list: List<Bundle.BundleEntryComponent>): DataResponseState<Boolean> {
        val bundle = Bundle().apply {
            entry = list
            type = Bundle.BundleType.TRANSACTION
        }

        return withContext(Dispatchers.IO) {
            try {
                val response = fhirClient.transaction().withBundle(bundle).execute()
//                if (BundleUtil.getBundleType()) {
//                    Logger.error("Failed to upload batch: ${response.code} - ${response.message}")
//                    return@withContext DataResponseState.Error(exceptionFromResponse(response))
//                }
                Logger.info("Uploaded successfully")
                DataResponseState.Success(true)
            } catch (e: Exception) {
                Logger.error("Failed to upload batch: ${e.message}")
                DataResponseState.Error(e)
            }
        }
    }

    private fun exceptionFromResponse(response: Response): Exception {
        return Exception("Status: ${response.code},message: ${response.message}, body: ${response.body?.string()} ")
    }
}

fun <T : IBaseResource> IQuery<Bundle>.paginateExecute(client: FhirClient): List<T> {
    val list = mutableListOf<IBaseResource>()

    var bundle = this.execute()
    list.addAll(BundleUtil.toListOfResources(client.ctx, bundle))

    while (bundle.getLink(IBaseBundle.LINK_NEXT) != null) {
        bundle = client.fhirClient.loadPage().next(bundle).execute();
        list.addAll(BundleUtil.toListOfResources(client.ctx, bundle));
    }

    return list.toList() as List<T>
}