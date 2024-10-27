package org.dtree.fhir.server.services.form

import org.dtree.fhir.core.models.TracingType
import org.dtree.fhir.core.uploader.general.FhirClient
import org.dtree.fhir.server.plugins.tasks.ChangeAppointmentData
import org.dtree.fhir.server.plugins.tasks.FinishVisitRequest
import org.hl7.fhir.r4.model.*
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
        val structureMap =
            fetcher.fetchStructureMap("structure_map/profile/patient_edit_profile/patient_edit_profile.map")
        val questionnaire: Questionnaire =
            fetcher.getQuestionnaire("questionnaire/profile/patient_edit_profile.json")
        val entriesToSave = mutableListOf<Resource>()

        for (entry in body) {
            val patientData = client.fetchAllPatientsActiveItems(entry.id)
            if (patientData.isEmpty()) continue
            val questionnaireResponse = responseGenerator.generateQuestionerResponse(questionnaire, patientData)

            val response = QuestionnaireResponseUpdater(questionnaireResponse)
            response.updateAnswerInGroup(
                "page-5",
                "careplan-end-date",
                listOf(QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                    value = DateTimeType(entry.date)
                })
            )

            val bundle = responseGenerator.extractBundle(questionnaire, questionnaireResponse, structureMap)
            questionnaireResponse.contained = bundle.entry.map { it.resource }

            entriesToSave.add(questionnaireResponse)
            entriesToSave.add(bundle)
        }

        saveResources(entriesToSave)
    }

    suspend fun tracingEnteredInError(patients: List<String>) {
        val entriesToSave = mutableListOf<Resource>()
        val phoneStructureMap =
            fetcher.fetchStructureMap("structure_map/tracing/phone_tracing/phone_tracing_outcomes.map")
        val homeStructureMap =
            fetcher.fetchStructureMap("structure_map/tracing/home_tracing/home_tracing_outcomes.map")
        val phoneQuestionnaire: Questionnaire =
            fetcher.getQuestionnaire("questionnaire/tracing/phone_tracing/phone_tracing_outcomes.json")
        val homeQuestionnaire: Questionnaire =
            fetcher.getQuestionnaire("questionnaire/tracing/home_tracing/home_tracing_outcomes.json")

        for (patientId in patients) {
            val patientData = client.fetchAllPatientsActiveItems(patientId)
            if (patientData.isEmpty()) continue
            val tracingType = patientData.getTracingType()
            if (tracingType == TracingType.none) continue

            val questionnaire = if (tracingType == TracingType.phone) phoneQuestionnaire else homeQuestionnaire
            val structureMap = if (tracingType == TracingType.phone) phoneStructureMap else homeStructureMap


            val questionnaireResponse = responseGenerator.generateQuestionerResponse(questionnaire, patientData)

            val response = QuestionnaireResponseUpdater(questionnaireResponse)
            response.updateSingleAnswer("is-tracing-conducted", BooleanType(false))
            response.updateAnswerInGroup(
                "not-conducted-group",
                "reason-for-no-tracing",
                listOf(QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                    value = Coding().apply {
                        code = "no-tracing-required"
                        display = "Error, this person does not require tracing"
                    }
                })
            )

            val bundle = responseGenerator.extractBundle(questionnaire, questionnaireResponse, structureMap)
            questionnaireResponse.contained = bundle.entry.map { it.resource }


            entriesToSave.add(questionnaireResponse)
            entriesToSave.add(bundle)
        }

        saveResources(entriesToSave)
    }

    suspend fun saveResources(resources: List<Resource>) {}
}