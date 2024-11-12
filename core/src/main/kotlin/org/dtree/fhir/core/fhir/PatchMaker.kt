package org.dtree.fhir.core.fhir

import ca.uhn.fhir.parser.IParser
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonpatch.diff.JsonDiff
import com.google.android.fhir.LocalChange
import com.google.android.fhir.LocalChangeToken
import com.google.android.fhir.sync.upload.patch.PatchOrdering.sccOrderByReferences
import org.dtree.fhir.core.utils.logicalId
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent
import org.hl7.fhir.r4.model.Resource
import org.json.JSONArray
import java.time.Instant
import java.util.UUID
import kotlin.random.Random

object PatchMaker {
    private val resourceHelper = FhirResourceHelper()
    fun createPatchedRequest(
        iParser: IParser,
        resourceMap: Map<String, Resource>,
        resources: List<Resource>
    ): List<BundleEntryComponent> {
        val changes = resources.mapNotNull { resource ->
            val resourceID = resource.logicalId
            var oldResource = resourceMap[resourceID]
            if (oldResource == null) {
                oldResource = resourceMap[resourceID.replace("#", "")]
            }
            if (oldResource == null) return@mapNotNull LocalChange(
                resourceType = resource.fhirType(),
                resourceId = resourceID,
                versionId = resource.meta.versionId,
                timestamp = Instant.now(),
                type = LocalChange.Type.INSERT,
                payload = iParser.encodeResourceToString(resource.apply {
                    id = logicalId
                }),
                token = LocalChangeToken(listOf(Random.nextLong())),
            )

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
                    payload = jsonDiff.second,
                    token = LocalChangeToken(listOf(Random.nextLong())),
                    isPatch = jsonDiff.first
                )
            }
        }
        val refs = changes.flatMap {
            val resource: Resource = if (it.isPatch) {
                resourceMap[it.resourceId]!!
            } else {
                iParser.parseResource(it.payload) as Resource
            }
            resourceHelper.extractLocalChangeWithRefs(it, resource)
        }
        val ordered = refs.sccOrderByReferences()
        val newLocalChanges = ordered.flatMap { it.patchMappings.map { it.localChange } }
        return newLocalChanges.map { it.createPatchRequest(iParser) }
    }

    private fun patch(parser: IParser, source: Resource, target: Resource): Pair<Boolean, String>? {
        val objectMapper = ObjectMapper()
        val sourceStr = objectMapper.readValue(parser.encodeResourceToString(source), JsonNode::class.java)
        val targetStr = objectMapper.readValue(parser.encodeResourceToString(target), JsonNode::class.java)

        val diff = JsonDiff.asJson(
            sourceStr,
            targetStr,
        )

        val response = getFilteredJSONArray(diff)
        val jsonDiff = response.first
        if (jsonDiff.length() == 0) {
            println("New resource same as last one")
            return null
        }

        return Pair(true, jsonDiff.toString())
    }

    private fun getFilteredJSONArray(jsonDiff: JsonNode): Pair<JSONArray, Boolean> {
        var hasMeta = false
        val ignorePaths = listOf("/meta", "/text")
        val array = with(JSONArray(jsonDiff.toString())) {
            return@with JSONArray(
                (0..<length())
                    .map { optJSONObject(it) }
                    .filterNot { jsonObject ->
                        val isRemove = jsonObject.optString("op")
                            .equals("remove")
                        if (jsonObject.optString("path").startsWith("/meta") && !isRemove) {
                            hasMeta = true
                        }
                        var paths = ignorePaths
                        if (hasMeta && !isRemove) {
                            paths = listOf("/text")
                        }
                        paths.any { jsonObject.optString("path").startsWith(it) } || isRemove
                    },
            )
        }
        return Pair(array, hasMeta)
    }
}