package org.dtree.fhir.server.core.util

import org.dtree.fhir.server.core.models.DateRange
import java.time.LocalDate
import java.time.ZoneId

fun dateListFromRange(value: DateRange?): List<LocalDate> {
    if (value != null) {
        val (from, to) = value
        if (from != null && to != null) {
            return from.datesUntil(to.plusDays(1))
                .toList()
        } else if (from != null) {
            return listOf(
                from
            )
        }
    }
    return listOf()
}