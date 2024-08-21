package org.dtree.fhir.core.tests.operations

import org.dtree.fhir.core.tests.inputs.TestTypes

class Contains : StringOperation() {
    override fun getName(): String {
        return TestTypes.Contains
    }

    override fun calculateValue(value: String, expected: String): Boolean {
        return value.contains(expected)
    }
}