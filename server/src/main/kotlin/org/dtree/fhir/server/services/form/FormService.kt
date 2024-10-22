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
        for (entry in body) {
            val patientData = client.fetchAllPatientsActiveItems(entry.id)
            if (patientData.isEmpty()) continue
            responseGenerator.generateFinishVisit(patientData.patient, CarePlan(), Date(), Date(), listOf())
        }
    }
}