package org.dtree.fhir.core.tests.operations

import org.dtree.fhir.core.tests.inputs.TestTypes

class NotContainsNoCase : StringOperation() {
    override fun getName(): String {
        return TestTypes.NotContainsNoCase
    }

    override fun calculateValue(value: String, expected: String): Boolean {
        return !value.contains(expected, ignoreCase = true)
    }
}