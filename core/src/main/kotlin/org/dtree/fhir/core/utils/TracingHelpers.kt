package org.dtree.fhir.core.utils


object TracingHelpers {
    private val tracingQuestionnaires: List<String> =
        listOf(
            "art-client-viral-load-test-results",
            "phone-tracing-outcome",
            "home-tracing-outcome",
            "art-client-welcome-service-high-or-detectable-viral-load",
            "art-client-viral-load-collection",
            "exposed-infant-convert-to-art-client",
            "patient-finish-visit",
            "exposed-infant-record-hiv-test-results",
            "art-client-child-contact-registration",
            "art-client-biological-parent-contact-registration",
            "art-client-child-contact-registration",
            "art-client-sexual-contact-registration",
            "art-client-sibling-contact-registration",
            "art-client-social-network-contact-registration",
            "contact-and-community-positive-hiv-test-and-next-appointment",
        )
    const val tracingBundleId = "tracing"

    fun requireTracingTasks(id: String): Boolean =
        tracingQuestionnaires.firstOrNull { x -> x == id } != null
}
