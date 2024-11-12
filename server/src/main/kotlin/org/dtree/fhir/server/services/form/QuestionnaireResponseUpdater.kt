package org.dtree.fhir.server.services.form

import com.google.android.fhir.datacapture.XFhirQueryResolver
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent
import org.hl7.fhir.r4.model.Type
import com.google.android.fhir.datacapture.enablement.EnablementEvaluator
import com.google.android.fhir.datacapture.extensions.localizedTextSpanned
import com.google.android.fhir.datacapture.extensions.unpackRepeatedGroups
import org.hl7.fhir.r4.model.Resource

typealias ItemToParentMap = MutableMap<QuestionnaireItemComponent, QuestionnaireItemComponent>

class QuestionnaireResponseUpdater(
    private val questionnaire: Questionnaire,
    private val questionnaireResponse: QuestionnaireResponse,
    private val questionnaireLaunchContextMap: Map<String, Resource>?,
    private var xFhirQueryResolver: XFhirQueryResolver,
) {
    private val enablementEvaluator: EnablementEvaluator
    private var questionnaireItemParentMap:
            Map<QuestionnaireItemComponent, QuestionnaireItemComponent>

    init {
        fun buildParentList(
            item: QuestionnaireItemComponent,
            questionnaireItemToParentMap: ItemToParentMap,
        ) {
            for (child in item.item) {
                questionnaireItemToParentMap[child] = item
                buildParentList(child, questionnaireItemToParentMap)
            }
        }

        questionnaireItemParentMap = buildMap {
            for (item in questionnaire.item) {
                buildParentList(item, this)
            }
        }

        enablementEvaluator = EnablementEvaluator(
            questionnaire,
            questionnaireResponse,
            questionnaireItemParentMap,
            questionnaireLaunchContextMap,
            xFhirQueryResolver,
        )
    }

    /**
     * Updates an answer in the QuestionnaireResponse by finding the matching linkId
     * Handles nested questions by recursively searching through item groups
     */
    fun updateAnswer(
        linkId: String,
        newAnswer: List<QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent>
    ): Boolean {
        return updateAnswerInItems(questionnaireResponse.item, linkId, newAnswer)
    }

    /**
     * Updates an answer by providing both parent group ID and child question ID
     */
    fun updateAnswerInGroup(
        groupId: String,
        childId: String,
        newAnswer: List<QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent>
    ): Boolean {
        // First find the parent group
        val group = findItemByLinkId(questionnaireResponse.item, groupId)
        if (group != null) {
            // Then update the child within that group
            return updateAnswerInItems(group.item, childId, newAnswer)
        }
        return false
    }

    /**
     * Updates a single answer within a specific group
     */
    fun updateSingleAnswerInGroup(groupId: String, childId: String, answerValue: Type): Boolean {
        val answer = QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent()
        answer.value = answerValue
        return updateAnswerInGroup(groupId, childId, listOf(answer))
    }

    /**
     * Finds an item component by its linkId
     */
    private fun findItemByLinkId(
        items: List<QuestionnaireResponse.QuestionnaireResponseItemComponent>,
        targetLinkId: String
    ): QuestionnaireResponse.QuestionnaireResponseItemComponent? {
        items.forEach { item ->
            if (item.linkId == targetLinkId) {
                return item
            }

            if (item.hasItem()) {
                findItemByLinkId(item.item, targetLinkId)?.let { return it }
            }

            if (item.hasAnswer()) {
                item.answer.forEach { answer ->
                    if (answer.hasItem()) {
                        findItemByLinkId(answer.item, targetLinkId)?.let { return it }
                    }
                }
            }
        }
        return null
    }

    /**
     * Convenience method to update a single answer value
     */
    fun updateSingleAnswer(linkId: String, answerValue: Type): Boolean {
        val answer = QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent()
        answer.value = answerValue
        return updateAnswer(linkId, listOf(answer))
    }

    private fun updateAnswerInItems(
        items: List<QuestionnaireResponse.QuestionnaireResponseItemComponent>,
        targetLinkId: String,
        newAnswer: List<QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent>
    ): Boolean {
        items.forEach { item ->
            // Check if current item matches the target linkId
            if (item.linkId == targetLinkId) {
                item.answer = newAnswer
                return true
            }

            // Recursively check nested items
            if (item.hasItem()) {
                if (updateAnswerInItems(item.item, targetLinkId, newAnswer)) {
                    return true
                }
            }

            // Check answers for nested items
            if (item.hasAnswer()) {
                item.answer.forEach { answer ->
                    if (answer.hasItem()) {
                        if (updateAnswerInItems(answer.item, targetLinkId, newAnswer)) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    /**
     * Retrieves an answer by linkId
     */
    fun getAnswer(linkId: String): List<QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent>? {
        return findAnswerInItems(questionnaireResponse.item, linkId)
    }

    /**
     * Retrieves an answer using both group ID and child ID
     */
    fun getAnswerFromGroup(
        groupId: String,
        childId: String
    ): List<QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent>? {
        val group = findItemByLinkId(questionnaireResponse.item, groupId)
        return group?.let { findAnswerInItems(it.item, childId) }
    }

    private fun findAnswerInItems(
        items: List<QuestionnaireResponse.QuestionnaireResponseItemComponent>,
        targetLinkId: String
    ): List<QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent>? {
        items.forEach { item ->
            if (item.linkId == targetLinkId) {
                return item.answer
            }

            if (item.hasItem()) {
                findAnswerInItems(item.item, targetLinkId)?.let { return it }
            }

            if (item.hasAnswer()) {
                item.answer.forEach { answer ->
                    if (answer.hasItem()) {
                        findAnswerInItems(answer.item, targetLinkId)?.let { return it }
                    }
                }
            }
        }
        return null
    }

    suspend fun getQuestionnaireResponse(): QuestionnaireResponse {
        return questionnaireResponse.copy().apply {
            // Use the view model's questionnaire and questionnaire response for calculating enabled items
            // because the calculation relies on references to the questionnaire response items.
            item =
                getEnabledResponseItems(
                    this@QuestionnaireResponseUpdater.questionnaire.item,
                    questionnaireResponse.item,
                )
                    .map { it.copy() }
            this.unpackRepeatedGroups(this@QuestionnaireResponseUpdater.questionnaire)
        }
    }

    private suspend fun getEnabledResponseItems(
        questionnaireItemList: List<QuestionnaireItemComponent>,
        questionnaireResponseItemList: List<QuestionnaireResponseItemComponent>,
    ): List<QuestionnaireResponseItemComponent> {
        val responseItemKeys = questionnaireResponseItemList.map { it.linkId }
        val result = mutableListOf<QuestionnaireResponseItemComponent>()

        for ((questionnaireItem, questionnaireResponseItem) in
        questionnaireItemList.zip(questionnaireResponseItemList)) {
            if (
                responseItemKeys.contains(questionnaireItem.linkId) &&
                enablementEvaluator.evaluate(questionnaireItem, questionnaireResponseItem)
            ) {
                questionnaireResponseItem.apply {
                    if (text.isNullOrBlank()) {
                        text = questionnaireItem.localizedTextSpanned
                    }
                    // Nested group items
                    item = getEnabledResponseItems(questionnaireItem.item, questionnaireResponseItem.item)
                    // Nested question items
                    answer.forEach { it.item = getEnabledResponseItems(questionnaireItem.item, it.item) }
                }
                result.add(questionnaireResponseItem)
            }
        }
        return result
    }
}