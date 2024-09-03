package org.dtree.fhir.server.util

import com.google.gson.*
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class LocalDateAdapter : JsonSerializer<LocalDate?>, JsonDeserializer<LocalDate?> {
    override fun serialize(src: LocalDate?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.format(formatter))
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDate? {
        return if (json != null) {
            LocalDate.parse(json.asJsonPrimitive.asString, formatter)
        } else null
    }

    companion object {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}