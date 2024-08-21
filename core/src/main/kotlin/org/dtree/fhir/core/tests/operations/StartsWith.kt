package org.dtree.fhir.core.tests.operations

import org.dtree.fhir.core.tests.inputs.TestTypes

class StartsWith : StringOperation() {
    override fun getName(): String {
        return TestTypes.StartsWith
    }

    override fun calculateValue(value: String, expected: String): Boolean {
        return value.startsWith(expected)
    }
}