package org.dtree.fhir.server.core.search.filters

import org.dtree.fhir.core.utils.extractId
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.QuestionnaireResponse

fun questionnaireParser(bundle: Bundle, linkId: String): Int {
    return bundle.entry.mapNotNull { et ->
        val entry = et.resource as QuestionnaireResponse
        if (entry.item.firstOrNull { item -> item.linkId == linkId }?.answer?.firstOrNull()?.valueBooleanType?.value == true) entry.subject.extractId() else null
    }.toSet().count()
}

fun uniquePatientQuestionnaireParser(bundle: Bundle): Int {
    return bundle.entry.mapNotNull { et ->
        (et.resource as QuestionnaireResponse).subject.extractId()
    }.toSet().count()
}