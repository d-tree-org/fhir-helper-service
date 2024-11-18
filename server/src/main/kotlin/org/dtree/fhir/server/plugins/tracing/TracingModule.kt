package org.dtree.fhir.server.plugins.tracing

import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dtree.fhir.server.controller.TracingController
import org.dtree.fhir.server.core.models.PaginatedResponse
import org.dtree.fhir.server.core.models.PaginationArgs
import org.dtree.fhir.server.services.tracing.TracingResult
import org.koin.ktor.ext.inject

fun Route.tracingModule() {
    val controller by inject<TracingController>()

    get<Tracing.Facility.Id> { facility ->
        val result = controller.getStats(facility.id)
        call.respond(result)
    }

    route("/tracing") {
        route("/facility/{id}") {

            route("all") {
                get(builder = {
                    operationId = "getAllList"
                    request {
                        pathParameter<String>("id") {
                            description = "Id"
                            required = true
                        }
                        queryParameter<Boolean>("all")
                        queryParameter<Int>("page")
                        queryParameter<Int>("pageSize")
                    }
                    response {
                        code(HttpStatusCode.OK) {
                            body<PaginatedResponse<TracingResult>> {

                            }
                        }
                    }
                }) {
                    val result = controller.getTracingList(
                        call.parameters["id"] ?: "",
                        PaginationArgs.parse(call.request.queryParameters)
                    )
                    call.respond(result)
                }
            }

            route("clean-future-date") {
                post() {
                    val result = controller.cleanFutureDateMissedAppointment(call.parameters["id"] ?: "")
                    call.respond(result)
                }
            }
        }

        route("entered-in-error") {
            post() {
                val patients = call.receive<List<String>>()
                if (patients.isEmpty()) return@post call.respond(HttpStatusCode.BadRequest, "Patients empty")
                call.respond("Job started")
                controller.setPatientsEnteredInError(patients)
            }
        }
    }


}