package org.dtree.fhir.server.services.patient

import org.dtree.fhir.core.models.PatientData
import org.dtree.fhir.core.uploader.general.FhirClient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object PatientService : KoinComponent {
    private val client by inject<FhirClient>()
    fun fetchPatientActiveResource(patientId: String): PatientData {
        return client.fetchAllPatientsActiveItems(patientId)
    }
}