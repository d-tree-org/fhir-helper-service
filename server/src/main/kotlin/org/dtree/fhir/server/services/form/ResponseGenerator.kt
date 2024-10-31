package org.dtree.fhir.server.services.form

import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.MustacheFactory
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.fhir.datacapture.mapping.StructureMapExtractionContext
import org.dtree.fhir.core.di.FhirProvider
import org.dtree.fhir.core.models.PatientData
import org.dtree.fhir.core.utilities.TransformSupportServices
import org.dtree.fhir.core.utils.asYyyyMmDd
import org.dtree.fhir.core.utils.category
import org.dtree.fhir.core.utils.logicalId
import org.hl7.fhir.r4.model.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.io.StringReader
import java.io.Writer
import java.util.*


class ResponseGenerator : KoinComponent {

    private val fetcher by inject<ResourceFetcher>()
    private val fhirProvider by inject<FhirProvider>()
    private val transformSupportServices = TransformSupportServices(fhirProvider.context)

    fun generateFinishVisit(
        patient: Patient,
        carePlan: CarePlan,
        nextAppointment: Date,
        dateVisited: Date,
        extras: List<Resource>
    ): QuestionnaireResponse {
        val template = fetcher.getResponseTemplate("finish-visit")
        val mf: MustacheFactory = DefaultMustacheFactory()
        val byteArrayOutputStream = ByteArrayOutputStream()

        val writer: Writer = OutputStreamWriter(byteArrayOutputStream)

        val mustache = mf.compile(StringReader(template), "patient-finish-visit")
        mustache.execute(
            writer, FinishVisitData(
                nextAppointment = nextAppointment.asYyyyMmDd(),
                dateVisited = dateVisited.asYyyyMmDd(),
                isClientAvailable = true,
                carePlanId = carePlan.logicalId,
                patient = FinishVisitData.Patient(
                    id = patient.logicalId,
                    category = patient.category,
                    birthDate = patient.birthDate.asYyyyMmDd(),
                ),
                contained = extras.joinToString(",") { fhirProvider.parser.encodeResourceToString(it) }
            )
        )
        writer.flush()
        val output = byteArrayOutputStream.toString()
        return fhirProvider.parser.parseResource(output) as QuestionnaireResponse
    }

    suspend fun generateQuestionerResponse(
        questionnaire: Questionnaire,
        patientData: PatientData
    ): QuestionnaireResponse {
        val populationResourcesList = patientData.toPopulationResource()
        val populationResourceTypeResourceMap =
            populationResourcesList.associateBy { it.resourceType.name.lowercase() }
        val response = ResourceMapper.populate(questionnaire, populationResourceTypeResourceMap)
        response.contained = populationResourcesList
        return response
    }

    suspend fun extractBundle(
        questionnaire: Questionnaire,
        questionnaireResponse: QuestionnaireResponse,
        structureMap: StructureMap
    ): Bundle {
        val targetResource = Bundle()
        fhirProvider.scu().transform(fhirProvider.context, questionnaireResponse, structureMap, targetResource)
        return targetResource
//        ResourceMapper.extract(
//            questionnaire = questionnaire,
//            questionnaireResponse = questionnaireResponse,
//            StructureMapExtractionContext(
//                transformSupportServices = transformSupportServices,
//                structureMapProvider = { _, _ ->
//                    return@StructureMapExtractionContext structureMap
//                },
//                workerContext = fhirProvider.context,
//            ),
//        )
    }
}