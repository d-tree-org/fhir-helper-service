package com.google.android.fhir

import ca.uhn.fhir.parser.IParser
import org.dtree.fhir.core.uploader.ContentTypes
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent
import org.hl7.fhir.r4.model.Resource
import java.time.Instant

data class LocalChange(
    /** The [ResourceType] */
    val resourceType: String,
    /** The resource id [Resource.id] */
    val resourceId: String,
    /** This is the id of the version of the resource that this local change is based of */
    val versionId: String? = null,
    /** The time instant the app user performed a CUD operation on the resource. */
    val timestamp: Instant,
    /** Type of local change like insert, delete, etc */
    val type: Type,
    /** json string with local changes */
    val payload: String,
    /**
     * This token value must be explicitly applied when list of local changes are squashed and
     * [LocalChange] class instance is created.
     */
    var token: LocalChangeToken,
    val isPatch: Boolean = false,
) {
    enum class Type(val value: Int) {
        INSERT(1), // create a new resource. payload is the entire resource json.
        UPDATE(2), // patch. payload is the json patch.
        DELETE(3), // delete. payload is empty string.
        ;

        companion object {
            fun from(input: Int): Type = values().first { it.value == input }
        }
    }

    fun createPatchRequest(
        iParser: IParser,
        resource: Resource? = null
    ): BundleEntryComponent {
        return if (type == LocalChange.Type.UPDATE) {
            if (isPatch) {
                createRequest(createPathRequest())
            } else {
                createRequest(iParser.parseResource(payload) as Resource)
            }
        } else if (resource != null && type == LocalChange.Type.INSERT) {
            createRequest(resource)
        } else {
            val resourceToUpload = iParser.parseResource(payload) as Resource
            createRequest(resourceToUpload)
        }
    }

    private fun createPathRequest(): Binary {
        return Binary().apply {
            contentType = ContentTypes.APPLICATION_JSON_PATCH
            data = payload.toByteArray()
        }
    }

    private fun createRequest(resourceToUpload: Resource): BundleEntryComponent {
        return BundleEntryComponent().apply {
            resource = resourceToUpload
            request = Bundle.BundleEntryRequestComponent().apply {
                url = "${resourceType}/${resourceId}"
                method = when (type) {
                    LocalChange.Type.INSERT -> Bundle.HTTPVerb.PUT
                    LocalChange.Type.UPDATE -> Bundle.HTTPVerb.PATCH
                    LocalChange.Type.DELETE -> Bundle.HTTPVerb.DELETE
                }
            }
        }
    }
}

data class LocalChangeToken(val ids: List<Long>)