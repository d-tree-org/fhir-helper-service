package org.dtree.fhir.server.plugins.careplan

import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dtree.fhir.server.controller.TracingController
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun Route.carePlanModule() {

    get<CarePlan.Facility.Id> { facility ->

    }
}