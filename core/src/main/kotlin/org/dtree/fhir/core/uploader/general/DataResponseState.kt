package org.dtree.fhir.core.uploader.general

sealed class DataResponseState<out T> {
    data class Success<T>(val data: T) : DataResponseState<T>()

    data class Error(val exception: Exception) : DataResponseState<Nothing>()
}