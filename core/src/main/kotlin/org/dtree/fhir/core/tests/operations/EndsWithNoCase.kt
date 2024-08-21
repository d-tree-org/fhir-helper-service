package org.dtree.fhir.core.tests.operations

import org.dtree.fhir.core.tests.inputs.TestTypes

class EndsWithNoCase : StringOperation() {
    override fun getName(): String {
        return TestTypes.EndsWithNoCase
    }

    override fun calculateValue(value: String, expected: String): Boolean {
        return value.endsWith(expected, ignoreCase = true)
    }
}