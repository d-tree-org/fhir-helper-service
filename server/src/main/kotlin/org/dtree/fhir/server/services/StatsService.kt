package org.dtree.fhir.server.services

import org.dtree.fhir.core.uploader.general.FhirClient
import org.dtree.fhir.server.core.search.filters.filterByLocation
import org.dtree.fhir.server.core.search.filters.patientTypeFilter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object StatsService : KoinComponent {
    val client by inject<FhirClient>()

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

        return fetchDataTest(client, listOf(newlyDiagnosed, alreadyOnArt, exposedInfants))
    }
}