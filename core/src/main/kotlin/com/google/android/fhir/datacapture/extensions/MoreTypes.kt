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

import org.hl7.fhir.r4.model.Attachment
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Expression
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.PrimitiveType
import org.hl7.fhir.r4.model.Quantity
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.Type
import org.hl7.fhir.r4.model.UriType


/**
 * Returns the string representation for [PrimitiveType] or [Quantity], otherwise defaults to null
 */
private fun getValueString(type: Type): String? =
    when (type) {
        is Quantity -> type.value?.toString()
        else -> (type as? PrimitiveType<*>)?.asStringValue()
    }

/** Converts StringType to toUriType. */
internal fun StringType.toUriType(): UriType {
    return UriType(value)
}

/** Converts StringType to CodeType. */
internal fun StringType.toCodeType(): CodeType {
    return CodeType(value)
}

/** Converts StringType to IdType. */
internal fun StringType.toIdType(): IdType {
    return IdType(value)
}

/** Converts Coding to CodeType. */
internal fun Coding.toCodeType(): CodeType {
    return CodeType(code)
}

/**
 * Converts Quantity to Coding type. The resulting Coding properties are equivalent of Coding.system
 * = Quantity.system Coding.code = Quantity.code Coding.display = Quantity.unit
 */
internal fun Quantity.toCoding(): Coding {
    return Coding(this.system, this.code, this.unit)
}

internal fun Type.hasValue(): Boolean = !getValueString(this).isNullOrBlank()

internal val Type.cqfCalculatedValueExpression
    get() = this.getExtensionByUrl(EXTENSION_CQF_CALCULATED_VALUE_URL)?.value as? Expression

internal const val EXTENSION_CQF_CALCULATED_VALUE_URL: String =
    "http://hl7.org/fhir/StructureDefinition/cqf-calculatedValue"
