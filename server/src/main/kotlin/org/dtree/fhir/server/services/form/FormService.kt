package org.dtree.fhir.server.services.form

import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.datacapture.XFhirQueryResolver
import org.dtree.fhir.core.models.TracingType
import org.dtree.fhir.core.uploader.general.FhirClient
import org.dtree.fhir.server.plugins.tasks.ChangeAppointmentData
import org.dtree.fhir.server.plugins.tasks.FinishVisitRequest
import org.dtree.fhir.server.plugins.tasks.TracingRemovalType
import org.hl7.fhir.r4.model.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

object FormService : KoinComponent {
    private val responseGenerator by inject<ResponseGenerator>()
    private val fetcher by inject<ResourceFetcher>()
    private val client by inject<FhirClient>()
    private val iParser by inject<IParser>()
    private val xFhirQueryResolver by inject<XFhirQueryResolver>()

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
            var questionnaireResponse = responseGenerator.generateQuestionerResponse(questionnaire, patientData)

            val responseUpdater = QuestionnaireResponseUpdater(
                questionnaire,
                questionnaireResponse,
                patientData.toLaunchContextMap(),
                xFhirQueryResolver,
            )
            responseUpdater.updateAnswerInGroup(
                "page-5",
                "careplan-end-date",
                listOf(QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                    value = DateTimeType(entry.date)
                })
            )
            questionnaireResponse = responseUpdater.getQuestionnaireResponse()
            val bundle = responseGenerator.extractBundle(questionnaire, questionnaireResponse, structureMap)
            questionnaireResponse.contained = bundle.entry.map { it.resource }

            entriesToSave.add(questionnaireResponse)
            entriesToSave.add(bundle)
        }

        saveResources(entriesToSave)
    }

    suspend fun tracingEnteredInError(patients: List<String>, type: TracingRemovalType) {
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


            var questionnaireResponse = responseGenerator.generateQuestionerResponse(questionnaire, patientData)

            val responseUpdater = QuestionnaireResponseUpdater(
                questionnaire,
                questionnaireResponse,
                patientData.toLaunchContextMap(),
                xFhirQueryResolver
            )
            responseUpdater.updateSingleAnswer("is-tracing-conducted", BooleanType(false))
            responseUpdater.updateAnswerInGroup(
                "not-conducted-group",
                "reason-for-no-tracing",
                listOf(QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                    if (type == TracingRemovalType.EnteredInError) {
                        value = Coding().apply {
                            code = "no-tracing-required"
                            display = "Error, this person does not require tracing"
                        }
                    } else if (type == TracingRemovalType.Deceased) {
                        value = Coding().apply {
                            code = "deceased"
                            display = "Deceased"
                        }
                    }
                })
            )
            questionnaireResponse = responseUpdater.getQuestionnaireResponse()
            println(iParser.encodeResourceToString(questionnaireResponse))
            val bundle = responseGenerator.extractBundle(questionnaire, questionnaireResponse, structureMap)
            val bundleResources = bundle.entry.map {
                it.resource
            }
            questionnaireResponse.contained = bundleResources

            entriesToSave.add(questionnaireResponse)
            entriesToSave.addAll(bundleResources)
        }

        saveResources(entriesToSave)
    }

    private suspend fun saveResources(resources: List<Resource>) {
        val bundle = Bundle()
        resources.forEach {
            bundle.addEntry().resource = it
        }
        println(iParser.encodeResourceToString(bundle))
//        client.bundleUpload(resources, 30)
    }
}