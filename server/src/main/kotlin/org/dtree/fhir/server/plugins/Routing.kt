package org.dtree.fhir.server.plugins

import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.github.smiley4.ktorswaggerui.routing.openApiSpec
import io.github.smiley4.ktorswaggerui.routing.swaggerUI
import io.ktor.server.application.*
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.get
import org.dtree.fhir.server.plugins.appointment.appointmentModule
import org.dtree.fhir.server.plugins.careplan.carePlanModule
import org.dtree.fhir.server.plugins.patient.patientModule
import org.dtree.fhir.server.plugins.scheduler.schedulerRoutes
import org.dtree.fhir.server.plugins.stats.statsModule
import org.dtree.fhir.server.plugins.tasks.tasksModule
import org.dtree.fhir.server.plugins.tracing.tracingModule

fun Application.configureRouting() {
    install(Resources)
    install(SwaggerUI) {
        info {
            title = "Example API"
            version = "latest"
            description = "Example API for testing and demonstration purposes."
        }
        server {
            url = "http://127.0.0.1:4040"
            description = "Development Server"
        }
    }
    routing {
        route("api.json") {
            openApiSpec()
        }
        // Create a route for the swagger-ui using the openapi-spec at "/api.json".
        route("swagger") {
            swaggerUI("/api.json")
        }
        authenticate("keycloakOAuth") {
            get("/secure") {
                call.respondText("You are authenticated!")
            }
        }

        statsModule()
        tracingModule()
        appointmentModule()
        carePlanModule()
        tasksModule()
        patientModule()
        schedulerRoutes()

        get("/") {
            call.respondText("Hello, World!")
        }
    }
}