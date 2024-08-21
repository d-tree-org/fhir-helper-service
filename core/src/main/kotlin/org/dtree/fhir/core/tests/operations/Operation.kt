package org.dtree.fhir.core.tests.operations

import org.dtree.fhir.core.models.TestStatus
import org.dtree.fhir.core.tests.inputs.PathResult

interface Operation {
    fun execute(value: PathResult?, expected: Any?): TestStatus
}