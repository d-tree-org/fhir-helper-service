package org.dtree.fhir.server.services.appointment

import org.dtree.fhir.core.uploader.general.FhirClient
import org.dtree.fhir.core.utils.logicalId
import org.dtree.fhir.server.core.models.FilterFormData
import org.dtree.fhir.server.core.search.filters.filterAddCount
import org.dtree.fhir.server.core.search.filters.filterByDate
import org.dtree.fhir.server.core.search.filters.filterByLocation
import org.dtree.fhir.server.core.search.filters.filterRevInclude
import org.dtree.fhir.server.services.tracing.AppointmentListResults
import org.dtree.fhir.server.services.tracing.Stuff
import org.dtree.fhir.server.services.tracing.fetch
import org.dtree.fhir.server.util.extractOfficialIdentifier
import org.hl7.fhir.r4.model.Appointment
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.ZoneId

class AppointmentService  : KoinComponent {
    private val client by inject<FhirClient>()

    fun getAppointmentList(facilityId: String, date: LocalDate): AppointmentListResults {
        val dateFilter = filterByDate(date)
        val locationFilter = filterByLocation(facilityId)
        val filter = FilterFormData(
            resource = ResourceType.Appointment.name,
            filterId = "random_filter",
            filters = listOf(dateFilter, filterAddCount(20000), filterRevInclude(), locationFilter)
        )

        val results = fetch(client, listOf(filter))
        return AppointmentListResults(results.map {
            val mPatient = (it.include as Patient)
            val appointment = it.main as Appointment
            val mDate = appointment.start?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
            Stuff(
                uuid = mPatient.logicalId,
                id = mPatient.extractOfficialIdentifier(),
                name = mPatient.nameFirstRep.nameAsSingleString,
                date = mDate
            )
        })
    }
}