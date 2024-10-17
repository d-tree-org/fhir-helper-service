package org.dtree.fhir.server.services.form

import org.hl7.fhir.r4.model.Patient
import java.time.LocalDate

class FormService(val fetcher: ResourceFetcher) {
    fun finishVisit(patient: Patient, date: LocalDate) {
        val strMap = fetcher.fetchStructureMap("finish-visit")
    }
}