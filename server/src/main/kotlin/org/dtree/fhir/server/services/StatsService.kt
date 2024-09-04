package org.dtree.fhir.server.services

import org.dtree.fhir.core.uploader.general.FhirClient
import org.dtree.fhir.server.core.search.filters.filterByLocation
import org.dtree.fhir.server.core.search.filters.patientTypeFilter
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

        return fetchDataTest(client, listOf(newlyDiagnosed, alreadyOnArt, exposedInfants)).let {
            val group = it.groups.toMutableList()
            group.add(
                GroupedSummaryItem(
                    groupKey = "visits",
                    groupTitle = "Today's visits",
                    summaries = listOf(
                        SummaryItem("All", 0),
                        SummaryItem("Already on Art", 0),
                        SummaryItem("Newly diagnosed", 0),
                        SummaryItem("Exposed Infant", 0),
                    ),
                )
            )
            group.add(
                GroupedSummaryItem(
                    groupKey = "tasks",
                    groupTitle = "Today's Tasks",
                    summaries = listOf(
                        SummaryItem("Milestone", 0),
                        SummaryItem("Viral Load Collected", 0),
                    ),
                )
            )
           it.copy(
                groups = group
            )
        }
    }
}