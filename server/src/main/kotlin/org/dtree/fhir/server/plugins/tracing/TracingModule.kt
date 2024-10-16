package org.dtree.fhir.server.plugins.tracing

import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dtree.fhir.server.controller.TracingController
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun Route.tracingModule() {
    val controller by inject<TracingController>()

    get<Tracing.Facility.Id> { facility ->
        val result = controller.getStats(facility.id)
        call.respond(result)
    }

    get<Tracing.Facility.Id.All> { values ->
        println("Jeff")
        val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        val result = controller.getTracingList(values.parent.id, if(values.date.isNullOrBlank()) LocalDate.now() else  LocalDate.parse(values.date, formatter))
        call.respond(result)
    }
}