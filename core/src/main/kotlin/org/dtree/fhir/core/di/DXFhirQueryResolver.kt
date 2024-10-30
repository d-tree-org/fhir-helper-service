package org.dtree.fhir.core.di

import com.google.android.fhir.datacapture.XFhirQueryResolver
import org.hl7.fhir.r4.model.Resource

class DXFhirQueryResolver() : XFhirQueryResolver {
    override suspend fun resolve(xFhirQuery: String): List<Resource> {
        // TODO: Implement resolution logic
        return emptyList()
    }
}