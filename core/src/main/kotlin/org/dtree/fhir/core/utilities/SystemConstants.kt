package org.dtree.fhir.core.utilities

object SystemConstants {
    const val REASON_CODE_SYSTEM = "https://d-tree.org/fhir/reason-code"
    const val BASE_URL = "https://d-tree.org"
    const val TASK_FILTER_TAG_SYSTEM = "https://d-tree.org/fhir/task-filter-tag"
    const val RESOURCE_CREATED_ON_TAG_SYSTEM = "https://d-tree.org/fhir/created-on-tag"
    const val TASK_TASK_ORDER_SYSTEM = "https://d-tree.org/fhir/clinic-visit-task-order"
    const val PATIENT_TYPE_FILTER_TAG_VIA_META_CODINGS_SYSTEM =
        "https://d-tree.org/fhir/patient-meta-tag"
    const val CONTACT_TRACING_SYSTEM = "https://d-tree.org/fhir/contact-tracing"
    const val OBSERVATION_CODE_SYSTEM = "https://d-tree.org/fhir/observation-codes"
    const val CARE_PLAN_REFERENCE_SYSTEM = "https://d-tree.org/fhir/careplan-reference"
    const val QUESTIONNAIRE_REFERENCE_SYSTEM = "https://d-tree.org/fhir/procedure-code"
    const val LOCATION_TAG = "http://smartregister.org/fhir/location-tag"
    const val RESOURCE_REMOVAL_REASON_SYSTEM = "https://d-tree.org/fhir/resource-removal-reason-code"
    const val LOCATION_HIERARCHY_BINARY = "location-hierarchy"

    fun getIdentifierSystemFromPatientType(patientType: String): String {
        return when (patientType) {
            "client-already-on-art",
            "newly-diagnosed-client", -> {
                "https://d-tree.org/fhir/patient-identifier-art"
            }
            "exposed-infant" -> {
                "https://d-tree.org/fhir/patient-identifier-hcc"
            }
            else -> {
                "https://d-tree.org/fhir/patient-identifier-hts"
            }
        }
    }

    fun getCodeByPriority(codes: List<String>): String? {
        val priorityOrder = listOf("client-already-on-art", "newly-diagnosed-client", "exposed-infant")

        if (codes.size == 1) {
            return codes[0]
        }

        for (priorityCode in priorityOrder) {
            if (codes.contains(priorityCode)) {
                return priorityCode
            }
        }

        return codes.firstOrNull()
    }
}