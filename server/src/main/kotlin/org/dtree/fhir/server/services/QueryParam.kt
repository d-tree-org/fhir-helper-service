package org.dtree.fhir.server.services

import org.dtree.fhir.core.utils.encodeUrl

class QueryParam(
    values: Map<String, String> = mapOf(), private val encodeUrl: Boolean = false
) {
    private val queries: MutableMap<String, String> = mutableMapOf()

    init {
        from(values)
    }

    fun add(key: String, value: Any) {
        var actualValue = value
        if (value is List<*>) {
            actualValue = value.joinToString(",")
        }
        if (queries.containsKey(key)) {
            queries["$key[${Math.random()}]"] = actualValue.toString()
        } else {
            queries[key] = actualValue.toString()
        }
    }

    fun get(key: String): String? {
        return queries[key]
    }

    fun set(key: String, value: Any) {
        add(key, value)
    }

    fun remove(key: String) {
        queries.remove(key)
    }

    private fun from(values: Map<String, String>) {
        for ((key, value) in values) {
            add(key, value)
        }
    }

    fun has(key: String): Boolean {
        return queries.containsKey(key)
    }

    fun fromArray(values: List<Map<String, String>>) {
        for (valueMap in values) {
            from(valueMap)
        }
    }

    fun toUrl(resources: String): String {
        val query = queries.map { (key, value) ->
            if (key.contains("[")) {
                "${key.split("[")[0]}=${if (encodeUrl) value.encodeUrl() else value}"
            } else {
                "$key=${if (encodeUrl) value.encodeUrl() else value}"
            }
        }.joinToString("&")
        return "$resources?$query"
    }
}