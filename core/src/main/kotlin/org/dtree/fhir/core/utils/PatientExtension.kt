package org.dtree.fhir.core.utils

import org.dtree.fhir.core.utilities.SystemConstants
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient

val Patient.category: String
    get() {
       return extractPatientTypeCoding()?.code ?: ""
    }


fun Patient.extractOfficialIdentifier(): String? {
    val patientTypes =
        this.meta.tag
            .filter { it.system == SystemConstants.PATIENT_TYPE_FILTER_TAG_VIA_META_CODINGS_SYSTEM }
            .map { it.code }
    val patientType: String? = SystemConstants.getCodeByPriority(patientTypes)
    return if (this.hasIdentifier() && patientType != null) {
        var actualId: Identifier? = null
        var hasNewSystem = false
        for (pId in this.identifier) {
            if (pId.system?.contains("https://d-tree.org/fhir/patient-identifier") == true) {
                hasNewSystem = true
            }
            if (pId.system == SystemConstants.getIdentifierSystemFromPatientType(patientType)) {
                actualId = pId
            }
        }
        if (!hasNewSystem) {
            this.identifier
                .lastOrNull { it.use == Identifier.IdentifierUse.OFFICIAL && it.system != "WHO-HCID" }
                ?.value
        } else {
            actualId?.value
        }
    } else {
        null
    }
}

fun Patient.extractPatientTypeCoding(): Coding? {
    val patientTypes =
        this.meta.tag.filter {
            it.system == SystemConstants.PATIENT_TYPE_FILTER_TAG_VIA_META_CODINGS_SYSTEM
        }
    val patientType: String? = SystemConstants.getCodeByPriority(patientTypes.map { it.code })
    return patientTypes.firstOrNull { patientType == it.code }
}