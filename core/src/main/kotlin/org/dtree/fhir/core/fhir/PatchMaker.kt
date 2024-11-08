package org.dtree.fhir.core.fhir

import ca.uhn.fhir.parser.IParser
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonpatch.diff.JsonDiff
import com.google.android.fhir.LocalChange
import com.google.android.fhir.LocalChangeToken
import org.dtree.fhir.core.utils.logicalId
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent
import org.hl7.fhir.r4.model.Resource
import org.json.JSONArray
import java.time.Instant

object PatchMaker {
    fun createPatchedRequest(
        iParser: IParser,
        resourceMap: Map<String, Resource>,
        resources: List<Resource>
    ): List<BundleEntryComponent> {
        return resources.mapNotNull { resource ->
            val resourceID = resource.logicalId
            val oldResource = resourceMap[resourceID]
                ?: return@mapNotNull LocalChange(
                    resourceType = resource.fhirType(),
                    resourceId = resourceID,
                    versionId = resource.meta.versionId,
                    timestamp = Instant.now(),
                    type = LocalChange.Type.INSERT,
                    payload = iParser.encodeResourceToString(resource.apply {
                        id = logicalId
                    }),
                    token = LocalChangeToken(listOf())
                ).createPatchRequest(iParser, resource)

            val jsonDiff = patch(iParser, oldResource.apply {
                id = logicalId
            }, resource.apply {
                id = logicalId
            })
            return@mapNotNull if (jsonDiff == null) {
                null
            } else {
                LocalChange(
                    resourceType = resource.fhirType(),
                    resourceId = resourceID,
                    versionId = resource.meta.versionId,
                    timestamp = Instant.now(),
                    type = LocalChange.Type.UPDATE,
                    payload = jsonDiff,
                    token = LocalChangeToken(listOf())
                ).createPatchRequest(iParser)
            }
        }
    }

    fun patch(iParser: IParser, oldResource: Resource, updatedResource: Resource): String? {
        val jsonDiff: JSONArray = diff(iParser, oldResource, updatedResource)
        if (jsonDiff.length() == 0) {
            println("New resource same as last one")
            return null
        }
        return jsonDiff.toString()
    }

    private fun diff(parser: IParser, source: Resource, target: Resource): JSONArray {
        val objectMapper = ObjectMapper()
        return getFilteredJSONArray(
            JsonDiff.asJson(
                objectMapper.readValue(parser.encodeResourceToString(source), JsonNode::class.java),
                objectMapper.readValue(parser.encodeResourceToString(target), JsonNode::class.java),
            ),
        )
    }

    private fun getFilteredJSONArray(jsonDiff: JsonNode) =
        with(JSONArray(jsonDiff.toString())) {
            val ignorePaths = setOf("/meta", "/text")
            return@with JSONArray(
                (0..<length())
                    .map { optJSONObject(it) }
                    .filterNot { jsonObject ->
                        ignorePaths.any { jsonObject.optString("path").startsWith(it) } || jsonObject.optString("op")
                            .equals("remove")
                    },
            )
        }
}