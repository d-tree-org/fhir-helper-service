package org.dtree.fhir.server.controller

import org.dtree.fhir.server.services.patient.PatientService
import org.hl7.fhir.r4.model.Bundle
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PatientControllerImpl : PatientController, BaseController(), KoinComponent {
    private val patientService by inject<PatientService>()
    override fun fetchPatientActiveResource(patientId: String): Bundle {
        return patientService.fetchPatientActiveResource(patientId).toBundle()
    }
}

interface PatientController {
    fun fetchPatientActiveResource(patientId: String): Bundle
}