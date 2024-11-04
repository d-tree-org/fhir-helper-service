package org.dtree.fhir.server.plugins.patient

import io.ktor.resources.*

@Resource("/patients")
class PatientResource {
    @Resource("{id}")
    class Id(val parent: PatientResource = PatientResource(), val id: String = "") {}
}