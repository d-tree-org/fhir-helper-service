package org.dtree.fhir.core.structureMaps

import org.dtree.fhir.core.fhir.FhirConfigs
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.StructureMap
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager
import org.hl7.fhir.utilities.npm.ToolsVersion
import org.dtree.fhir.core.utils.readFile

fun createStructureMapFromString(data: String, srcName: String): StructureMap? {
    val contextR4 = FhirConfigs.createWorkerContext()
    val scu = org.hl7.fhir.r4.utils.StructureMapUtilities(contextR4)
    return scu.parse(data, srcName)
}

fun createStructureMapFromFile(path: String, srcName: String): StructureMap? {
    val fileData = path.readFile()
    return createStructureMapFromString(fileData, srcName)
}