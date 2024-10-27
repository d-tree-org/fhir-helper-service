package org.dtree.fhir.server.services.form

import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Type

class QuestionnaireResponseUpdater(
    private val questionnaireResponse: QuestionnaireResponse
) {
    /**
     * Updates an answer in the QuestionnaireResponse by finding the matching linkId
     * Handles nested questions by recursively searching through item groups
     */
    fun updateAnswer(linkId: String, newAnswer: List<QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent>): Boolean {
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
}