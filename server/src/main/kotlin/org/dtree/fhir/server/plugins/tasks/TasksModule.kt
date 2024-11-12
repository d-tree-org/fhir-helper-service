package org.dtree.fhir.server.plugins.tasks

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import org.dtree.fhir.server.controller.TasksController
import org.koin.ktor.ext.inject

fun Route.tasksModule() {
    val controller by inject<TasksController>()

    post<Tasks.Fixes.FinishVisits> {
        val body = call.receive<List<FinishVisitRequest>>()
        controller.finishVisits(body)
        call.respond("Jeff")
    }

    post<Tasks.Fixes.AppointmentData> {
        val body = call.receive<List<ChangeAppointmentData>>()
        controller.changeAppointmentData(body)
        call.respond("Jeff")
    }

    post<Tasks.Fixes.TracingEnteredError> {
        val body = call.receive<TracingEnteredErrorData>()

        controller.tracingEnteredInError(body.data, body.type)

        call.respond("Jeff")
    }

    post<Tasks.Fixes.ChangeStatus> {
        val body = call.receive<ChangeStatusData>()

        controller.changeStatus(body.data, body.type)

        call.respond("Jeff")
    }

    get<Tasks.Utils> {
        controller.runUtil()
        call.respond("Jeff")
    }
}