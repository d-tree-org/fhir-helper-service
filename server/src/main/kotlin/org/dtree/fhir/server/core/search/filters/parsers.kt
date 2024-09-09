package org.dtree.fhir.server.core.search.filters

import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.QuestionnaireResponse

fun questionnaireParser(bundle: Bundle, linkId: String): Int {
    return bundle.entry.mapNotNull { et ->
        val entry = et.resource as QuestionnaireResponse
        if (entry.item.firstOrNull { item -> item.linkId == linkId }?.answer?.firstOrNull()?.valueBooleanType?.value == true) entry else null
    }.count()
}