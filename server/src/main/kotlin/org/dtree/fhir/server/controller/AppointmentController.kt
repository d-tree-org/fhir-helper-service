package org.dtree.fhir.server.controller

import org.dtree.fhir.server.services.appointment.AppointmentService
import org.dtree.fhir.server.services.appointment.AppointmentListResults
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate

class  AppointmentControllerImpl : AppointmentController, BaseController(), KoinComponent {
    private val appointmentService by inject<AppointmentService>()

    override fun getAppointmentList(facilityId: String, date: LocalDate): AppointmentListResults {
        return appointmentService.getAppointmentList(facilityId, date)
    }
}
interface AppointmentController {
    fun getAppointmentList(facilityId: String, date: LocalDate) : AppointmentListResults
}