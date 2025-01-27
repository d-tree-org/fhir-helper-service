package org.dtree.fhir.server.services.form

import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.datacapture.XFhirQueryResolver
import org.apache.commons.lang3.time.DateUtils
import org.dtree.fhir.core.fhir.PatchMaker
import org.dtree.fhir.core.models.PatientData
import org.dtree.fhir.core.models.TracingType
import org.dtree.fhir.core.uploader.general.FhirClient
import org.dtree.fhir.core.utils.asYyyyMmDd
import org.dtree.fhir.core.utils.category
import org.dtree.fhir.core.utils.createBundleComponent
import org.dtree.fhir.server.plugins.tasks.ChangeAppointmentData
import org.dtree.fhir.server.plugins.tasks.ChangeStatusType
import org.dtree.fhir.server.plugins.tasks.FinishVisitRequest
import org.dtree.fhir.server.plugins.tasks.TracingRemovalType
import org.hl7.fhir.r4.model.*
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent
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

    suspend fun changeStatus(patients: List<String>, type: ChangeStatusType) {
        val entriesToSave = mutableListOf<BundleEntryComponent>()

        val structureMapChangeStatus: StructureMap =
            fetcher.fetchStructureMap("structure_map/change_status/change_status.map")
        val statusQuestionnaire: Questionnaire =
            fetcher.getQuestionnaire("questionnaire/profile/change_status.json")

        val structureMapMilestone =
            fetcher.fetchStructureMap("structure_map/visit/exposed_infant/milestone_hiv_test/exposed_infant_milestone_hiv_test.map")
        val questionnaireMilestone: Questionnaire =
            fetcher.getQuestionnaire("questionnaire/visit/exposed_infant/milestone_hiv_test/exposed_infant_milestone_hiv_test.json")

        for (patientId in patients) {
            val patientData = client.fetchAllPatientsActiveItems(patientId)
            if (patientData.isEmpty()) continue
            val questionnaire: Questionnaire
            val structureMap: StructureMap
            val exposedDischarge =
                type == ChangeStatusType.Discharged && patientData.patient.category == "exposed-infant"
            if (exposedDischarge) {
                questionnaire = questionnaireMilestone.copy()
                structureMap = structureMapMilestone
            } else {
                questionnaire = statusQuestionnaire.copy()
                structureMap = structureMapChangeStatus
            }
            entriesToSave.addAll(handleParse(
                questionnaire,
                structureMap,
                patientData
            ) {
                if (exposedDischarge) {
                    updateAnswerInGroup("page-1", "able-to-conduct-test", listOf(
                        QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                            value = BooleanType(true)
                        }
                    ))
                    updateAnswerInGroup("page-1", "test-type", listOf(
                        QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                            value = Coding().apply {
                                code = "dbs"
                                display = "DBS"
                            }
                        }
                    ))
                    updateAnswerInGroup("page-1", "are-dbs-test-results-available", listOf(
                        QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                            value = BooleanType(true)
                        }
                    ))
                    updateAnswerInGroup("page-1", "hiv-test-results", listOf(
                        QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                            value = Coding().apply {
                                code = "negative"
                                display = "Negative"
                            }
                        }
                    ))
                    updateAnswerInGroup("page-1", "is-child-weaned", listOf(
                        QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                            value = Coding().apply {
                                code = "yes"
                                display = "Yes"
                            }
                        }
                    ))
                    updateAnswerInGroup("page-1", "when-was-child-weaned", listOf(
                        QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                            value = Coding().apply {
                                code = "more-than-6-weeks-ago"
                                display = "More than 6 weeks ago"
                            }
                        }
                    ))
                } else if (type == ChangeStatusType.Discharged) {
                    updateSingleAnswer("why-are-you-removing-this-person-art-client", Coding().apply {
                        code = "negative-confirmatory-hiv-test"
                        display = "Their confirmatory HIV test came back with a negative result"
                    })
                }
            })
        }

        saveResources(entriesToSave)
    }

    suspend fun changeAppointmentData(body: List<ChangeAppointmentData>) {
        val structureMap =
            fetcher.fetchStructureMap("structure_map/profile/patient_edit_profile/patient_edit_profile.map")
        val questionnaireRaw: Questionnaire =
            fetcher.getQuestionnaire("questionnaire/profile/patient_edit_profile.json")
        val entriesToSave = mutableListOf<BundleEntryComponent>()

        for (entry in body) {
            val questionnaire = questionnaireRaw.copy()
            val patientData = client.fetchAllPatientsActiveItems(entry.id)
            if (patientData.isEmpty()) continue
            var questionnaireResponse = responseGenerator.generateQuestionerResponse(questionnaire, patientData)

            if (patientData.currentCarePlan?.period?.end != null && DateUtils.isSameDay(
                    patientData.currentCarePlan?.period?.end,
                    entry.date
                )
            ) {
                println("The date is already the same ${entry.id} - ${entry.date.asYyyyMmDd()}")
                continue
            }

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
            println(iParser.encodeResourceToString(questionnaireResponse))
            val bundle = responseGenerator.extractBundle(questionnaire, questionnaireResponse, structureMap)
            println(iParser.encodeResourceToString(bundle))
            val bundleResources = bundle.entry.map {
                val resource = it.resource
                if (resource is Appointment) {
                    val idx =
                        resource.participant.indexOfFirst { it.actor?.reference?.contains("Practitioner/Practitioner/") == true }
                    if (idx != -1) {
                        resource.participant[idx] = resource.participant[idx].apply {
                            actor.reference = actor.reference.replace("Practitioner/Practitioner/", "Practitioner/")
                        }
                    }
                }
                resource
            }
            questionnaireResponse.contained = bundleResources

            entriesToSave.add(questionnaireResponse.createBundleComponent())
            entriesToSave.addAll(PatchMaker.createPatchedRequest(iParser, patientData.getAllItemMap(), bundleResources))
        }
        saveResources(entriesToSave)
    }

    suspend fun tracingEnteredInError(patients: List<String>, type: TracingRemovalType) {
        val entriesToSave = mutableListOf<BundleEntryComponent>()
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

            val questionnaire =
                if (tracingType == TracingType.phone) phoneQuestionnaire.copy() else homeQuestionnaire.copy()
            val structureMap = if (tracingType == TracingType.phone) phoneStructureMap else homeStructureMap


            var questionnaireResponse = responseGenerator.generateQuestionerResponse(questionnaire, patientData)

            val responseUpdater = QuestionnaireResponseUpdater(
                questionnaire,
                questionnaireResponse,
                patientData.toLaunchContextMap(),
                xFhirQueryResolver
            )
            if (type != TracingRemovalType.TransferredOut) {
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
            } else {
                responseUpdater.updateSingleAnswer("is-tracing-conducted", BooleanType(true))
                responseUpdater.updateAnswerInGroup(
                    "conducted-group",
                    "tracing-outcome",
                    listOf(QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                        value = Coding().apply {
                            code = "transfer-out"
                            display = "Transfer Out"
                        }
                    })
                )
            }
            questionnaireResponse = responseUpdater.getQuestionnaireResponse()
            println(iParser.encodeResourceToString(questionnaireResponse))
            val bundle = responseGenerator.extractBundle(questionnaire, questionnaireResponse, structureMap)
            val bundleResources = bundle.entry.map { it.resource }
            questionnaireResponse.contained = bundleResources

            entriesToSave.add(questionnaireResponse.createBundleComponent())
            entriesToSave.addAll(PatchMaker.createPatchedRequest(iParser, patientData.getAllItemMap(), bundleResources))
        }

        saveResources(entriesToSave)
    }

    private suspend fun saveResources(resources: List<BundleEntryComponent>) {
        if (resources.isEmpty()) return
        val bundle = Bundle()
        resources.forEach {
            bundle.addEntry(it)
        }
        println(iParser.encodeResourceToString(bundle))
//        throw Exception("Jeff")
        client.bundleUpload(resources, 30)
    }

    private suspend fun handleParse(
        questionnaire: Questionnaire,
        structureMap: StructureMap,
        patientData: PatientData,
        updateResponse: QuestionnaireResponseUpdater.() -> Unit
    ): List<BundleEntryComponent> {
        var questionnaireResponse = responseGenerator.generateQuestionerResponse(questionnaire, patientData)

        val responseUpdater = QuestionnaireResponseUpdater(
            questionnaire,
            questionnaireResponse,
            patientData.toLaunchContextMap(),
            xFhirQueryResolver
        )
        updateResponse.invoke(responseUpdater)
        questionnaireResponse = responseUpdater.getQuestionnaireResponse()
        println(iParser.encodeResourceToString(questionnaireResponse))
        val bundle = responseGenerator.extractBundle(questionnaire, questionnaireResponse, structureMap)
        val bundleResources = bundle.entry.mapNotNull { it.resource }
        questionnaireResponse.contained = bundleResources

        return listOf(questionnaireResponse.createBundleComponent()) + PatchMaker.createPatchedRequest(
            iParser,
            patientData.getAllItemMap(),
            bundleResources
        )
    }

}