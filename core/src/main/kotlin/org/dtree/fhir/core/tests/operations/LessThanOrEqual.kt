package org.dtree.fhir.core.tests.operations

import org.dtree.fhir.core.tests.inputs.TestTypes
import java.math.BigDecimal

class LessThanOrEqual : NumberOperations() {

    override fun getName(): String {
        return TestTypes.LessThanOrEqual
    }

    override fun calculateValue(value: BigDecimal, expected: BigDecimal): Boolean {
        return value <= expected
    }
}