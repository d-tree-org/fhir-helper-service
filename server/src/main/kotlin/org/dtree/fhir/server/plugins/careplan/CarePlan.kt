package org.dtree.fhir.server.plugins.careplan

import io.ktor.resources.*
import org.dtree.fhir.server.plugins.tracing.Tracing

@Resource("/careplan")
class CarePlan {
    @Resource("facility")
    class Facility(val parent: Tracing = Tracing()) {
        @Resource("{id}")
        class Id(val parent: Facility = Facility(), val id: String = "") {

            @Resource("all")
            class All(val parent: Id = Facility.Id(), val date: String? = "")
        }
    }
}