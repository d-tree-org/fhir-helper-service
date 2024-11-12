package org.dtree.fhir.core.utils

import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*

val SDF_DD_MMM_YYYY = SimpleDateFormat("dd-MMM-yyyy")
val SDF_YYYY_MM_DD = SimpleDateFormat("yyyy-MM-dd")
val SDF_DD_MM_YYYY = SimpleDateFormat("dd/MM/yyyy")

fun parseDate(value: String?, useIso: Boolean = false): LocalDateTime? {
    return try {
        value?.let {
            var date = it
            if (it.contains("+")) {
                date = it.split("+").firstOrNull() ?: date
            }
            LocalDateTime.parse(
                date,
                // TODO: Fix this
               // if (useIso) DateTimeFormatter.ISO_LOCAL_DATE_TIME else DateTimeFormatter.ofPattern("dd-MM-yyyy")
            )
        }
    } catch (e: Exception) {
        null
    }
}

fun Date.asDdMmmYyyy(): String {
    return SDF_DD_MMM_YYYY.format(this)
}

fun Date.asDdMmYyyy(): String {
    return SDF_DD_MM_YYYY.format(this)
}

fun Date.asYyyyMmDd(): String {
    return SDF_YYYY_MM_DD.format(this)
}