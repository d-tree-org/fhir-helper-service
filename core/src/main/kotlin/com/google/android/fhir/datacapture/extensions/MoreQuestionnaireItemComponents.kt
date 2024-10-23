/*
 * Copyright 2023-2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.fhir.datacapture.extensions

import org.hl7.fhir.r4.model.*

// Please note these URLs do not point to any FHIR Resource and are broken links. They are being
// used until we can engage the FHIR community to add these extensions officially.

internal const val EXTENSION_ITEM_CONTROL_URL_ANDROID_FHIR =
    "https://github.com/google/android-fhir/StructureDefinition/questionnaire-itemControl"

internal const val ITEM_INITIAL_EXPRESSION_URL: String =
    "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression"

internal const val EXTENSION_VARIABLE_URL = "http://hl7.org/fhir/StructureDefinition/variable"

internal const val EXTENSION_ITEM_CONTROL_URL =
    "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl"

internal const val EXTENSION_ITEM_CONTROL_SYSTEM = "http://hl7.org/fhir/questionnaire-item-control"

/**
 * Creates a list of [QuestionnaireResponse.QuestionnaireResponseItemComponent]s corresponding to
 * the nested items under the questionnaire item.
 *
 * The list can be added as nested items under answers in a corresponding questionnaire response
 * item. This may be because
 * 1. the questionnaire item is a question with nested questions, in which case each answer in the
 *    questionnaire response item needs to have the same nested questions, or
 * 2. the questionnaire item is a repeated group, in which case each answer in the questionnaire
 *    response item represents an instance of the repeated group, and needs to have the same nested
 *    questions.
 *
 * The hierarchy and order of child items will be retained as specified in the standard. See
 * https://www.hl7.org/fhir/questionnaireresponse.html#notes for more details.
 */
internal fun Questionnaire.QuestionnaireItemComponent.createNestedQuestionnaireResponseItems() =
    item.map { it.createQuestionnaireResponseItem() }



// ********************************************************************************************** //
//                                                                                                //
// Utilities: zip with questionnaire response item list, nested items, create response items,     //
// flattening, etc.                                                                               //
//                                                                                                //
// ********************************************************************************************** //

/**
 * Returns a list of values built from the elements of `this` and the
 * `questionnaireResponseItemList` with the same linkId using the provided `transform` function
 * applied to each pair of questionnaire item and questionnaire response item.
 *
 * In case of repeated group item, `questionnaireResponseItemList` will contain
 * QuestionnaireResponseItemComponent with same linkId. So these items are grouped with linkId and
 * associated with its questionnaire item linkId.
 */
internal inline fun <T> List<Questionnaire.QuestionnaireItemComponent>.zipByLinkId(
    questionnaireResponseItemList: List<QuestionnaireResponse.QuestionnaireResponseItemComponent>,
    transform:
        (
        Questionnaire.QuestionnaireItemComponent,
        QuestionnaireResponse.QuestionnaireResponseItemComponent,
    ) -> T,
): List<T> {
    val linkIdToQuestionnaireResponseItemListMap = questionnaireResponseItemList.groupBy { it.linkId }
    return flatMap { questionnaireItem ->
        linkIdToQuestionnaireResponseItemListMap[questionnaireItem.linkId]?.mapNotNull {
                questionnaireResponseItem ->
            transform(questionnaireItem, questionnaireResponseItem)
        }
            ?: emptyList()
    }
}

internal val Questionnaire.QuestionnaireItemComponent.isRepeatedGroup: Boolean
    get() = type == Questionnaire.QuestionnaireItemType.GROUP && repeats
// TODO: Move this elsewhere.
val Resource.logicalId: String
    get() {
        return this.idElement?.idPart.orEmpty()
    }

/**
 * The initial-expression extension on [QuestionnaireItemComponent] to allow dynamic selection of
 * default or initially selected answers
 */
val Questionnaire.QuestionnaireItemComponent.initialExpression: Expression?
    get() {
        return this.extension
            .firstOrNull { it.url == ITEM_INITIAL_EXPRESSION_URL }
            ?.let { it.value as Expression }
    }

/**
 * Creates a corresponding [QuestionnaireResponse.QuestionnaireResponseItemComponent] for the
 * questionnaire item with the following properties:
 * - same `linkId` as the questionnaire item,
 * - any initial answer(s) specified either in the `initial` element or as `initialSelected`
 *   `answerOption`(s),
 * - any nested questions under the initial answers (there will be no user input yet since this is
 *   just being created) if this is a question with nested questions, and
 * - any nested questions if this is a non-repeated group.
 *
 * Note that although initial answers to a repeated group may be interpreted as initial instances of
 * the repeated group in the in-memory representation of questionnaire response, they are not
 * defined as such in the standard. As a result, we are not treating them as such in this function
 * to be conformant.
 *
 * The hierarchy and order of child items will be retained as specified in the standard. See
 * https://www.hl7.org/fhir/questionnaireresponse.html#notes for more details.
 */
internal fun Questionnaire.QuestionnaireItemComponent.createQuestionnaireResponseItem():
        QuestionnaireResponse.QuestionnaireResponseItemComponent {
    return QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
        linkId = this@createQuestionnaireResponseItem.linkId
        answer = createQuestionnaireResponseItemAnswers()
        if (
            type != Questionnaire.QuestionnaireItemType.GROUP &&
            this@createQuestionnaireResponseItem.item.isNotEmpty() &&
            answer.isNotEmpty()
        ) {
            this.copyNestedItemsToChildlessAnswers(this@createQuestionnaireResponseItem)
        } else if (
            this@createQuestionnaireResponseItem.type == Questionnaire.QuestionnaireItemType.GROUP &&
            !repeats
        ) {
            this@createQuestionnaireResponseItem.item.forEach {
                if (!it.isRepeatedGroup) {
                    this.addItem(it.createQuestionnaireResponseItem())
                }
            }
        }
    }
}

/**
 * Returns a list of answers from the initial values of the questionnaire item. `null` if no initial
 * value.
 */
private fun Questionnaire.QuestionnaireItemComponent.createQuestionnaireResponseItemAnswers():
        MutableList<QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent>? {
    // TODO https://github.com/google/android-fhir/issues/2161
    // The rule can be by-passed if initial value was set by an initial-expression.
    // The [ResourceMapper] at L260 wrongfully sets the initial property of questionnaire after
    // evaluation of initial-expression.
    require(answerOption.isEmpty() || initial.isEmpty() || initialExpression != null) {
        "Questionnaire item $linkId has both initial value(s) and has answerOption. See rule que-11 at https://www.hl7.org/fhir/questionnaire-definitions.html#Questionnaire.item.initial."
    }

    // https://build.fhir.org/ig/HL7/sdc/behavior.html#initial
    // quantity given as initial without value is for unit reference purpose only. Answer conversion
    // not needed
    if (
        answerOption.initialSelected.isEmpty() &&
        (initial.isEmpty() ||
                (initialFirstRep.hasValueQuantity() && initialFirstRep.valueQuantity.value == null))
    ) {
        return null
    }

    if (
        type == Questionnaire.QuestionnaireItemType.GROUP ||
        type == Questionnaire.QuestionnaireItemType.DISPLAY
    ) {
        throw IllegalArgumentException(
            "Questionnaire item $linkId has initial value(s) and is a group or display item. See rule que-8 at https://www.hl7.org/fhir/questionnaire-definitions.html#Questionnaire.item.initial.",
        )
    }

    if ((answerOption.initialSelected.size > 1 || initial.size > 1) && !repeats) {
        throw IllegalArgumentException(
            "Questionnaire item $linkId can only have multiple initial values for repeating items. See rule que-13 at https://www.hl7.org/fhir/questionnaire-definitions.html#Questionnaire.item.initial.",
        )
    }

    return initial
        .map { it.value }
        .plus(answerOption.initialSelected)
        .map { QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply { value = it } }
        .toMutableList()
}

// ********************************************************************************************** //
//                                                                                                //
// Additional display utilities: display item control, localized text spanned,                    //
// localized prefix spanned, localized instruction spanned, etc.                                  //
//                                                                                                //
// ********************************************************************************************** //

/** UI controls relevant to rendering questionnaire items. */
internal enum class DisplayItemControlType(val extensionCode: String) {
    FLYOVER("flyover"),
    PAGE("page"),
    HELP("help"),
}

/** Item control to show instruction text */
internal val Questionnaire.QuestionnaireItemComponent.displayItemControl: DisplayItemControlType?
    get() {
        val codeableConcept =
            this.extension.firstOrNull { it.url == EXTENSION_ITEM_CONTROL_URL }?.value as CodeableConcept?
        val code =
            codeableConcept?.coding?.firstOrNull { it.system == EXTENSION_ITEM_CONTROL_SYSTEM }?.code
        return DisplayItemControlType.values().firstOrNull { it.extensionCode == code }
    }