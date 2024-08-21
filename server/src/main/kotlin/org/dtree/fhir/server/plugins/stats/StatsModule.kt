package org.dtree.fhir.server.plugins.stats

import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dtree.fhir.server.controller.StatsController
import org.koin.ktor.ext.inject

fun Route.statsModule() {
    val controller by inject<StatsController>()

    get<Stats.Facility.Id> { facility ->
        val result = controller.getStats(facility.id)
        call.respond(result)
    }
}