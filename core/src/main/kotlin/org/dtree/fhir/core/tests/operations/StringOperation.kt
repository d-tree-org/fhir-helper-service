package org.dtree.fhir.core.tests.operations

import org.dtree.fhir.core.models.TestStatus
import org.dtree.fhir.core.tests.inputs.PathResult
import org.dtree.fhir.core.tests.inputs.testTypeNameMap

open class StringOperation : Operation {

    open fun getName(): String {
        TODO("Implement this in the class that inherits")
    }

    open fun calculateValue(value: String, expected: String): Boolean {
        TODO("Implement this in the class that inherits")
    }

    override fun execute(value: PathResult?, expected: Any?): TestStatus {
        return if (value?.value is String && expected is String) {
            var error: Exception? = null
            val passed: Boolean = calculateValue(value.value.toString(), expected)
            if (!passed)
                error = Exception("Expected: $value ${testTypeNameMap[getName()]} $expected")
            TestStatus(
                passed = passed, value = value, expected = expected, exception = error
            )
        } else {
            TestStatus(
                passed = false, value = value, expected = expected, exception = Exception("Expected String")
            )
        }
    }
}