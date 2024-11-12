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

package com.google.android.fhir

import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Expression
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.PrimitiveType
import org.hl7.fhir.r4.model.Quantity
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.Type
import org.hl7.fhir.r4.model.UriType
import java.util.*


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

/**
 * Returns whether two instances of the [Type] class are equal.
 *
 * Note this is not an operator because it is not possible to overload the equality operator as an
 * extension.
 */
fun equals(a: Type, b: Type): Boolean {
    if (a::class != b::class) return false

    if (a === b) return true

    if (a.isPrimitive) return a.primitiveValue() == b.primitiveValue()

    // Codes with the same system and code values are considered equal even if they have different
    // display values.
    if (a is Coding && b is Coding) return a.system == b.system && a.code == b.code

    throw NotImplementedError("Comparison for type ${a::class.java} not supported.")
}

internal fun Type.hasValue(): Boolean = !getValueString(this).isNullOrBlank()

internal val Type.cqfCalculatedValueExpression
    get() = this.getExtensionByUrl(EXTENSION_CQF_CALCULATED_VALUE_URL)?.value as? Expression

internal const val EXTENSION_CQF_CALCULATED_VALUE_URL: String =
    "http://hl7.org/fhir/StructureDefinition/cqf-calculatedValue"

operator fun Type.compareTo(value: Type): Int {
    if (!this.fhirType().equals(value.fhirType())) {
        throw IllegalArgumentException(
            "Cannot compare different data types: ${this.fhirType()} and ${value.fhirType()}",
        )
    }
    when {
        this.fhirType().equals("integer") -> {
            return this.primitiveValue().toInt().compareTo(value.primitiveValue().toInt())
        }
        this.fhirType().equals("decimal") -> {
            return this.primitiveValue().toBigDecimal().compareTo(value.primitiveValue().toBigDecimal())
        }
        this.fhirType().equals("date") -> {
            return clearTimeFromDateValue(this.dateTimeValue().value)
                .compareTo(clearTimeFromDateValue(value.dateTimeValue().value))
        }
        this.fhirType().equals("dateTime") -> {
            return this.dateTimeValue().value.compareTo(value.dateTimeValue().value)
        }
        this.fhirType().equals("Quantity") -> {
            val quantity =
                UnitConverter.getCanonicalFormOrOriginal(UcumValue((this as Quantity).code, this.value))
            val anotherQuantity =
                UnitConverter.getCanonicalFormOrOriginal(UcumValue((value as Quantity).code, value.value))
            if (quantity.code != anotherQuantity.code) {
                throw IllegalArgumentException(
                    "Cannot compare different quantity codes: ${quantity.code} and ${anotherQuantity.code}",
                )
            }
            return quantity.value.compareTo(anotherQuantity.value)
        }
        else -> {
            throw NotImplementedError()
        }
    }
}

private fun clearTimeFromDateValue(dateValue: Date): Date {
    val calendarValue = Calendar.getInstance()
    calendarValue.time = dateValue
    calendarValue.set(Calendar.HOUR_OF_DAY, 0)
    calendarValue.set(Calendar.MINUTE, 0)
    calendarValue.set(Calendar.SECOND, 0)
    calendarValue.set(Calendar.MILLISECOND, 0)
    return calendarValue.time
}

fun StringType.getLocalizedText(lang: String = Locale.getDefault().toLanguageTag()): String? {
    return getTranslation(lang) ?: getTranslation(lang.split("-").firstOrNull()) ?: value
}
