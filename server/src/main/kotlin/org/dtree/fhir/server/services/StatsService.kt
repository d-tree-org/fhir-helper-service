package org.dtree.fhir.server.services

import org.dtree.fhir.core.uploader.general.FhirClient
import org.dtree.fhir.server.core.models.FilterFormData
import org.dtree.fhir.server.core.search.filters.filterByDateCreated
import org.dtree.fhir.server.core.search.filters.filterByLocation
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.util.*

object StatsService: KoinComponent {
    val client by inject<FhirClient>()
   suspend fun getFacilityStats(id: String) {
       val filterFormData = FilterFormData(
           listOf(
               filterByLocation(id),
               filterByDateCreated(LocalDate.now())
           )
       )
       val data = fetchDataTest(filterFormData)
    }
}