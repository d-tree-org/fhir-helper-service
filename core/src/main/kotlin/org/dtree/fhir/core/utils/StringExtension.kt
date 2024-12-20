package org.dtree.fhir.core.utils

fun String.replaceTemplate(values: Map<String, String>): String {
    var string = this
    values.forEach { (key, value) ->
        string = string.replace("{{$key}}", value)
    }
    return string
}

fun String.encodeUrl(): String {
    return java.net.URLEncoder.encode(this, "UTF-8")
}