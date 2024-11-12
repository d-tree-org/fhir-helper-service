package org.dtree.fhir.server.plugins.appointment

import io.ktor.resources.*


@Resource("/appointments")
class Appointment() {
    @Resource("facility")
    class Facility(val parent: Appointment = Appointment()) {
        @Resource("{id}")
        class Id(val parent: Facility = Facility(), val id: String = "") {
            @Resource("all")
            class List(val parent: Id = Facility.Id(), val date: String? = "")
        }
    }
}