package org.dtree.fhir.server.plugins.careplan

import io.ktor.resources.*

@Resource("/careplan")
class CarePlan {
    @Resource("facility")
    class Facility(val parent: CarePlan = CarePlan()) {
        @Resource("{id}")
        class Id(val parent: Facility = Facility(), val id: String = "") {

            @Resource("all")
            class All(val parent: Id = Facility.Id(), val date: String? = "")
        }
    }
}