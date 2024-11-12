package org.dtree.fhir.core.di

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.parser.IParser
import org.dtree.fhir.core.fhir.FhirConfigs
import org.dtree.fhir.core.utilities.TransformSupportServices
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.utils.StructureMapUtilities

class FhirProvider {
    fun scu(): StructureMapUtilities {
        return  StructureMapUtilities(context, TransformSupportServices(context))
    }

    val parser: IParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
    val context = FhirConfigs.createWorkerContext()
}