package org.dtree.fhir.server.services

import org.dtree.fhir.core.uploader.general.FhirClient
import org.dtree.fhir.server.core.search.filters.addPatientFilter
import org.dtree.fhir.server.core.search.filters.filterByLocation
import org.dtree.fhir.server.core.search.filters.patientTypeFilter
import org.dtree.fhir.server.core.search.filters.questionnaireResponseFilters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object StatsService : KoinComponent {
    private val client by inject<FhirClient>()

    suspend fun getFacilityStats(id: String): FacilityResultData {
        val locationFilter = filterByLocation(id)
        val baseFilters = listOf(locationFilter)

        val newlyDiagnosed =
            patientTypeFilter(listOf(PatientType.NEWLY_DIAGNOSED_CLIENT), baseFilters).copy(
                groupId = "totals",
                title = "Newly diagnosed clients (all)"
            )
        val alreadyOnArt =
            patientTypeFilter(listOf(PatientType.CLIENT_ALREADY_ON_ART), baseFilters).copy(
                groupId = "totals",
                title = "Already on Art (all)"
            )
        val exposedInfants = patientTypeFilter(listOf(PatientType.EXPOSED_INFANT), baseFilters).copy(
            groupId = "totals",
            title = "Exposed infant (all)"
        )

        val allVisits = questionnaireResponseFilters("patient-finish-visit", baseFilters).copy(
            groupId = "visits",
            title = "All finish visits"
        )

        val allVisitsExposed = questionnaireResponseFilters(
            "patient-finish-visit",
            (baseFilters + addPatientFilter(listOf(PatientType.EXPOSED_INFANT), inSubject = true))
        ).copy(
            groupId = "visits",
            title = "All finish visits"
        )

        val allVisitsArt = questionnaireResponseFilters(
            "patient-finish-visit",
            (baseFilters + addPatientFilter(listOf(PatientType.EXPOSED_INFANT), inSubject = true))
        ).copy(
            groupId = "visits",
            title = "All finish visits"
        )

        val allVisitsNewly = questionnaireResponseFilters(
            "patient-finish-visit",
            (baseFilters + addPatientFilter(listOf(PatientType.EXPOSED_INFANT), inSubject = true))
        ).copy(
            groupId = "visits",
            title = "All finish visits"
        )

        val milestone = questionnaireResponseFilters("exposed-infant-milestone-hiv-test", baseFilters).copy(
            groupId = "tasks",
            title = "Milestone conducted"
        )

        val viralLoad = questionnaireResponseFilters("art-client-viral-load-collection", baseFilters).copy(
            groupId = "tasks",
            title = "Viral Load Collected"
        )

        return fetchDataTest(
            client, listOf(
                newlyDiagnosed, alreadyOnArt, exposedInfants,
                allVisits, allVisitsExposed, allVisitsNewly, allVisitsArt,
                milestone, viralLoad
            )
        )
    }
}