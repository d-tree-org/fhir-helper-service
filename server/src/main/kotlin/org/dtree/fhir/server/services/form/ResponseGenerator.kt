package org.dtree.fhir.server.services.form

import ca.uhn.fhir.parser.IParser
import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.MustacheFactory
import org.dtree.fhir.core.utils.asYyyyMmDd
import org.dtree.fhir.core.utils.category
import org.dtree.fhir.core.utils.logicalId
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.io.StringReader
import java.io.Writer
import java.util.*


class ResponseGenerator : KoinComponent {
    private val fetcher by inject<ResourceFetcher>()
    private val iParser by inject<IParser>()
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
                contained = extras.joinToString(",") { iParser.encodeResourceToString(it) }
            )
        )
        writer.flush()
        val output = byteArrayOutputStream.toString()
        return iParser.parseResource(output) as QuestionnaireResponse
    }
}