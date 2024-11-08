package org.dtree.fhir.server.plugins.tasks

import io.ktor.resources.*

@Resource("/tasks")
class Tasks() {
    @Resource("fixes")
    class Fixes(val parent: Tasks = Tasks()) {
        @Resource("finish-visits")
        class FinishVisits(val parent: Fixes = Fixes())

        @Resource("appointment-date")
        class AppointmentData(val parent: Fixes = Fixes())

        @Resource("tracing-entered-error")
        class TracingEnteredError(val parent: Fixes = Fixes())
    }
    @Resource("util")
    class Utils(val parent: Tasks = Tasks()) {}
}