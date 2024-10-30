package com.google.android.fhir.datacapture

import org.hl7.fhir.r4.model.Resource

/**
 * Resolves resources based on the provided xFhir query. This allows the library to resolve
 * x-fhir-query answer expressions.
 *
 * NOTE: The result of the resolution may be cached to improve performance. In other words, the
 * resolver may be called only once after which the Resources may be used multiple times in the UI.
 */
fun interface XFhirQueryResolver {
    suspend fun resolve(xFhirQuery: String): List<Resource>
}