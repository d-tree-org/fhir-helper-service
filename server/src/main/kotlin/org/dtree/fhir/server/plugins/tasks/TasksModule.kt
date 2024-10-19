package org.dtree.fhir.server.plugins.tasks

import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dtree.fhir.server.controller.TasksController
import org.koin.ktor.ext.inject

fun Route.tasksModule() {
    val controller by inject<TasksController>()
    get<Tasks.Fixes.FinishVisits> {
        controller.finishVisits()
        call.respond("Jeff")
    }
}