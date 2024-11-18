package org.dtree.fhir.server.plugins.tracing

import io.ktor.resources.*

@Resource("/tracing")
class Tracing() {
    @Resource("facility")
    class Facility(val parent: Tracing = Tracing()) {
        @Resource("{id}")
        class Id(val parent: Facility = Facility(), val id: String = "") {
            @Resource("appointments")
            class List(val parent: Id = Facility.Id(), val date: String? = "")

            @Resource("all")
            class All(val parent: Id = Facility.Id(), val all: Boolean = true, val page: Int = 0, val pageSize: Int = 0)

            @Resource("clean-future-date")
            class CleanFutureDate(val parent: Id = Facility.Id())
        }
    }
    @Resource("entered-in-error")
    class EnteredInError(val parent: Tracing = Tracing()) {}
}