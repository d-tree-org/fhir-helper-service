package org.dtree.fhir.core.tests.operations

import org.dtree.fhir.core.tests.inputs.TestTypes

class EndsWith : StringOperation() {
    override fun getName(): String {
        return TestTypes.EndsWith
    }

    override fun calculateValue(value: String, expected: String): Boolean {
        return value.endsWith(expected)
    }
}