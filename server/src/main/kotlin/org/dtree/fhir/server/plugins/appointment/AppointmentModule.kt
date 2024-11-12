package org.dtree.fhir.server.plugins.appointment

import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dtree.fhir.server.controller.AppointmentController
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun Route.appointmentModule() {
    val controller by inject<AppointmentController>()

    get<Appointment.Facility.Id.List> { appointment ->
        println("Jeff")
        val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        val result = controller.getAppointmentList(appointment.parent.id, if(appointment.date.isNullOrBlank()) LocalDate.now() else  LocalDate.parse(appointment.date, formatter))
        call.respond(result)
    }
}