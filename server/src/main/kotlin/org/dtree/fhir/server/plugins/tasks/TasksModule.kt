package org.dtree.fhir.server.plugins.tasks

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dtree.fhir.server.controller.TasksController
import org.koin.ktor.ext.inject

fun Route.tasksModule() {
    val controller by inject<TasksController>()
    get<Tasks.Fixes.FinishVisits> {
        val body = call.receive<List<FinishVisitRequest>>()
        controller.finishVisits(body)
        call.respond("Jeff")
    }
}