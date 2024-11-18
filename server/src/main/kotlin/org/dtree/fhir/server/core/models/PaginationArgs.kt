package org.dtree.fhir.server.core.models

data class PaginationArgs(
    val all: Boolean = false,
    val page: Int = 0,
    val pageSize: Int = 0,
) {
    companion object {
        fun parse(params:io.ktor.http.Parameters): PaginationArgs {
            val all = params["all"]?.toBoolean() ?: true
            val page = params["page"]?.toInt() ?: 0
            val pageSize = params["pageSize"]?.toInt() ?: 0

            return PaginationArgs(
                all = all, page = page, pageSize = pageSize
            )
        }
    }
}