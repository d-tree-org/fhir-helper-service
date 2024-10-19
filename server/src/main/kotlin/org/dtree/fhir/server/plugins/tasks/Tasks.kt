package org.dtree.fhir.server.plugins.tasks

import io.ktor.resources.*

@Resource("/tasks")
class Tasks() {
    @Resource("fixes")
    class Fixes(val parent: Tasks = Tasks()) {
        @Resource("finish-visits")
        class FinishVisits(val parent: Fixes = Fixes())
    }
}