package org.dtree.fhir.server.plugins.tracing

import io.ktor.resources.*
import java.time.LocalDate

@Resource("/tracing")
class Tracing() {
    @Resource("facility")
    class Facility(val parent: Tracing = Tracing()) {
        @Resource("{id}")
        class Id(val parent: Facility = Facility(), val id: String = "") {
            @Resource("appointments")
            class List(val parent: Id = Facility.Id(), val date: String? = "")
        }
    }
}