package org.dtree.fhir.server.services.form

import org.dtree.fhir.core.utils.asReference
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.util.*

object FormService : KoinComponent {
    private val responseGenerator by inject<ResponseGenerator>()
    private val fetcher by inject<ResourceFetcher>()

    fun finishVisit() {
        val strMap = fetcher.fetchStructureMap("finish-visit")
        responseGenerator.generateFinishVisit(Patient(), Date(), Date())
    }
}