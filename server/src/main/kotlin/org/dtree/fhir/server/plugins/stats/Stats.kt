package org.dtree.fhir.server.plugins.stats

import io.ktor.resources.*

@Resource("/stats")
class Stats() {
    @Resource("facility")
    class Facility(val parent: Stats = Stats()) {
        @Resource("{id}")
        class Id(val parent: Facility = Facility(), val id: String)
    }
}