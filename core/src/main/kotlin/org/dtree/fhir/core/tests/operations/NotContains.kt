package org.dtree.fhir.core.tests.operations

import org.dtree.fhir.core.tests.inputs.TestTypes

class NotContains : StringOperation() {
    override fun getName(): String {
        return TestTypes.NotContains
    }

    override fun calculateValue(value: String, expected: String): Boolean {
        return !value.contains(expected)
    }
}