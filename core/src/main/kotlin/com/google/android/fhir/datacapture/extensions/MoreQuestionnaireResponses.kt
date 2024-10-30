package com.google.android.fhir.datacapture.extensions

import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse

/** Pre-order list of all questionnaire response items in the questionnaire. */
val QuestionnaireResponse.allItems: List<QuestionnaireResponse.QuestionnaireResponseItemComponent>
    get() = item.flatMap { it.descendant }

 fun QuestionnaireResponse.unpackRepeatedGroups(questionnaire: Questionnaire) {
    item = unpackRepeatedGroups(questionnaire.item, item)
}

private fun unpackRepeatedGroups(
    questionnaireItems: List<Questionnaire.QuestionnaireItemComponent>,
    questionnaireResponseItems: List<QuestionnaireResponse.QuestionnaireResponseItemComponent>,
): List<QuestionnaireResponse.QuestionnaireResponseItemComponent> {
    return questionnaireItems
        .zipByLinkId(questionnaireResponseItems) { questionnaireItem, questionnaireResponseItem ->
            unpackRepeatedGroups(questionnaireItem, questionnaireResponseItem)
        }
        .flatten()
}

private fun unpackRepeatedGroups(
    questionnaireItem: Questionnaire.QuestionnaireItemComponent,
    questionnaireResponseItem: QuestionnaireResponse.QuestionnaireResponseItemComponent,
): List<QuestionnaireResponse.QuestionnaireResponseItemComponent> {
    questionnaireResponseItem.item =
        unpackRepeatedGroups(questionnaireItem.item, questionnaireResponseItem.item)
    questionnaireResponseItem.answer.forEach {
        it.item = unpackRepeatedGroups(questionnaireItem.item, it.item)
    }
    return if (questionnaireItem.isRepeatedGroup) {
        questionnaireResponseItem.answer.map {
            QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
                linkId = questionnaireItem.linkId
                text = questionnaireItem.localizedTextSpanned
                item = it.item
            }
        }
    } else {
        listOf(questionnaireResponseItem)
    }
}
