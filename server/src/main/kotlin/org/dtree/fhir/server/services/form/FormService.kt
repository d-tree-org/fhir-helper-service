package org.dtree.fhir.server.services.form

import org.dtree.fhir.core.uploader.general.FhirClient
import org.dtree.fhir.server.plugins.tasks.ChangeAppointmentData
import org.dtree.fhir.server.plugins.tasks.FinishVisitRequest
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.Resource
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

    suspend fun changeAppointmentData(body: List<ChangeAppointmentData>) {
        val structureMap = fetcher.fetchStructureMap("structure_map/profile/patient_edit_profile/patient_edit_profile.map")
        val entriesToSave = mutableListOf<Resource>()
        for (entry in body) {
            val patientData = client.fetchAllPatientsActiveItems(entry.id)
            if (patientData.isEmpty()) continue
            val questionnaire: Questionnaire =
                fetcher.getQuestionnaire("questionnaire/profile/patient_edit_profile.json")
            val questionnaireResponse = responseGenerator.generateQuestionerResponse(questionnaire, patientData)
            val bundle = responseGenerator.extractBundle(questionnaire, questionnaireResponse, structureMap)
            questionnaireResponse.contained = bundle.entry.map { it.resource }
            entriesToSave.add(questionnaireResponse)
            entriesToSave.add(bundle)
        }
    }
}