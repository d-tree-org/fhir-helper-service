package org.dtree.fhir.core.utilities

import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding

object ReasonConstants {
    // TODO: change code  to "welcome-service"
    val WelcomeServiceCode =
        CodeableConcept(Coding(SystemConstants.REASON_CODE_SYSTEM, "Welcome", "Welcome Service"))
            .apply { text = "Welcome Service" }

    val homeTracingCoding =
        Coding(SystemConstants.CONTACT_TRACING_SYSTEM, "home-tracing", "Home Tracing")
    val phoneTracingCoding =
        Coding(SystemConstants.CONTACT_TRACING_SYSTEM, "phone-tracing", "Phone Tracing")

    var missedAppointmentTracingCode =
        Coding(SystemConstants.REASON_CODE_SYSTEM, "missed-appointment", "Missed Appointment")
    var missedMilestoneAppointmentTracingCode =
        Coding(SystemConstants.REASON_CODE_SYSTEM, "missed-milestone", "Missed Milestone Appointment")
    var missedRoutineAppointmentTracingCode =
        Coding(SystemConstants.REASON_CODE_SYSTEM, "missed-routine", "Missed Routine Appointment")
    var interruptedTreatmentTracingCode =
        Coding(SystemConstants.REASON_CODE_SYSTEM, "interrupted-treatment", "Interrupted Treatment")

    var pendingTransferOutCode =
        Coding("https://d-tree.org/fhir/transfer-out-status", "pending", "Pending")

    const val TRACING_OUTCOME_CODE = "tracing-outcome"
    const val DATE_OF_AGREED_APPOINTMENT = "date-of-agreed-appointment"

    val resourceEnteredInError =
        Coding(SystemConstants.RESOURCE_REMOVAL_REASON_SYSTEM, "entered-in-error", "Entered in error")
}