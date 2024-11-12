package org.dtree.fhir.server.plugins.patient

import ca.uhn.fhir.parser.IParser
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.dtree.fhir.server.controller.PatientController
import org.koin.ktor.ext.inject

fun Route.patientModule() {
    val controller by inject<PatientController>()
    val iParser by inject<IParser>()

    get<PatientResource.Id> { patient ->
        val bundle = controller.fetchPatientActiveResource(patient.id)
        if (bundle == null) {
            call.respond(HttpStatusCode.BadRequest, "Could not find patient")
            return@get
        }
        call.respondText(
            iParser.encodeResourceToString(bundle),
            ContentType.Application.Json,
            HttpStatusCode.OK
        )
    }
}