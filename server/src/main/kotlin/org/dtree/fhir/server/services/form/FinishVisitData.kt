package org.dtree.fhir.server.services.form

data class FinishVisitData(
    val patient: Patient,
    val isClientAvailable: Boolean,
    val carePlanId: String,
    val dateVisited: String,
    val nextAppointment: String,
    val patientLinkArrayContainsPatientAtIndex0: Boolean = false,
    val patientLinkArrayContainsPatientAtIndex1: Boolean = false,
    val patientLinkArrayContainsRelatedpersonAtIndex0: Boolean = false,
    val patientLinkArrayContainsRelatedpersonAtIndex1: Boolean = false,
    val patientLinkRefer: Int = 0,
    val patientLinkSeealso: Int = 0,
    val contained: String? = null
) {
    data class Patient(val id: String, val category: String, val birthDate: String)
}