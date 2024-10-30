package org.dtree.fhir.core.utils

import org.dtree.fhir.core.utilities.ReasonConstants
import org.hl7.fhir.r4.model.Task

fun Task.isHomeTracingTask(): Boolean {
  return this.meta.tag.firstOrNull {
    it.`is`(ReasonConstants.homeTracingCoding.system, ReasonConstants.homeTracingCoding.code)
  } !== null
}
