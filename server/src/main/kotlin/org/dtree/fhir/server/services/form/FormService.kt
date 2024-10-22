package org.dtree.fhir.server.services.form

import org.dtree.fhir.core.uploader.general.FhirClient
import org.dtree.fhir.core.utils.logicalId
import org.dtree.fhir.server.plugins.tasks.FinishVisitRequest
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

object FormService : KoinComponent {
    private val responseGenerator by inject<ResponseGenerator>()
    private val fetcher by inject<ResourceFetcher>()
    private val client by inject<FhirClient>()

    fun finishVisit(body: List<FinishVisitRequest>) {
        val strMap = fetcher.fetchStructureMap("finish-visit")
        val patientMap = mapOf(*client.fetchResourcesFromList(ResourceType.Patient, body.map { it.id }).entry.map {
            val patient = it.resource as Patient
            Pair(patient.logicalId, patient)
        }.toTypedArray())
        for (entry in body) {
            val patient = patientMap[entry.id] ?: continue
            responseGenerator.generateFinishVisit(patient, CarePlan(), Date(), Date(), listOf())
        }
    }
}