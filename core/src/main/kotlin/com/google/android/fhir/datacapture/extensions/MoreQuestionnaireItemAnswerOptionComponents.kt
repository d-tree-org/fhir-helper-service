package com.google.android.fhir.datacapture.extensions

import org.hl7.fhir.r4.model.Attachment
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemAnswerOptionComponent
import org.hl7.fhir.r4.model.Type

/** Get the answer options values with `initialSelected` set to true */
internal val List<Questionnaire.QuestionnaireItemAnswerOptionComponent>.initialSelected: List<Type>
    get() = this.filter { it.initialSelected }.map { it.value }