package org.dtree.fhir.core.di

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.parser.IParser
import org.dtree.fhir.core.fhir.FhirConfigs
import org.hl7.fhir.r4.context.SimpleWorkerContext

class FhirProvider {
    fun parser(): IParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
    fun context(): SimpleWorkerContext = FhirConfigs.createWorkerContext()
}