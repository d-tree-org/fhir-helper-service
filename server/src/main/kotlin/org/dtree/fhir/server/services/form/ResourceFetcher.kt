package org.dtree.fhir.server.services.form

import org.dtree.fhir.core.utils.readFile
import org.hl7.fhir.r4.model.StructureMap

interface ResourceFetcher {
    fun fetchStructureMap(id: String): StructureMap
    fun getResponseTemplate(s: String): String
}

class LocalResourceFetcher : ResourceFetcher {
    override fun fetchStructureMap(id: String): StructureMap {
        return StructureMap()
    }

    override fun getResponseTemplate(s: String): String {
        return "D:\\Work\\dev\\fhir-resources\\response-templates\\finish-visit.mustache".readFile()
    }
}