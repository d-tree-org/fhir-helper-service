package org.dtree.fhir.server.core.cache

import org.dtree.fhir.server.core.models.PaginatedResponse
import kotlin.math.ceil

class PaginationUtil<T> {
    fun paginate(items: List<T>, page: Int, pageSize: Int): PaginatedResponse<T> {
        val totalItems = items.size
        val totalPages = ceil(totalItems.toDouble() / pageSize).toInt()
        val startIndex = (page - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, totalItems)

        val paginatedItems = if (startIndex < totalItems) {
            items.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        return PaginatedResponse(
            items = paginatedItems,
            page = page,
            pageSize = pageSize,
            totalItems = totalItems,
            totalPages = totalPages
        )
    }
}