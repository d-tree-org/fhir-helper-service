package org.dtree.fhir.core.uploader

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.parser.IParser
import org.dtree.fhir.core.compiler.parsing.ParseJsonCommands
import org.dtree.fhir.core.config.ProjectConfig
import org.dtree.fhir.core.config.ProjectConfigManager
import org.dtree.fhir.core.fhir.FhirConfigs
import org.dtree.fhir.core.uploader.general.FhirClient
import org.dtree.fhir.core.utilities.TransformSupportServices
import org.dtree.fhir.core.utils.Logger
import org.dtree.fhir.core.utils.readFile
import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.utils.StructureMapUtilities
import java.io.File
import java.nio.file.Paths


class FileUploader() {
    private val iParser: IParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
    private val scu: StructureMapUtilities
    private lateinit var dotenv: Dotenv
    private lateinit var uploader: FhirClient
    private var contextR4: SimpleWorkerContext = FhirConfigs.createWorkerContext()
    private lateinit var projectConfig: ProjectConfig
    private val uploadList = mutableListOf<File>()
    private val excludeList = mutableSetOf<String>()

    init {
        scu = StructureMapUtilities(contextR4, TransformSupportServices(contextR4))
    }


    suspend fun batchUpload(directoryPath: String, projectRoot: String) {
        dotenv = dotenv {
            directory = projectRoot
        }
        val configManager = ProjectConfigManager()
        projectConfig = configManager.loadProjectConfig(projectRoot, directoryPath)
        processExcludedPaths(projectRoot)
        uploader = FhirClient(dotenv, iParser)
        fetchFiles(
            directoryPath, mapOf(
                Pair(projectConfig.structureMapLocation, "map"),
                Pair(projectConfig.questionnaireMapLocation, "json")
            )
        )
        Logger.info("Found ${uploadList.size} files")
        uploadToFhirServer(uploadList)
    }

    private fun fetchFiles(directoryPath: String, maps: Map<String, String>) {
        for (map in maps) {
            val path = Paths.get(directoryPath, map.key).toAbsolutePath()
            val baseDirectory = path.toFile()

            if (baseDirectory.exists() && baseDirectory.isDirectory) {
                processFiles(baseDirectory, map.value)
            } else {
                Logger.error("Directory does not exist or is not a directory")
            }
        }
    }

    private fun compileMapToJson(mapFile: File): Resource? {
        return ParseJsonCommands().parseStructureMap(mapFile.absolutePath, iParser, scu, projectConfig)
    }

    private fun compileQuestionnaire(file: File): Resource? {
        return try {
            return iParser.parseResource(Questionnaire::class.java, file.readFile())
        } catch (e: Exception) {
            Logger.error("Path: ${file.path} Error: $e")
            null
        }
    }

    private fun createBundleEntry(res: Resource): BundleEntryComponent {
        val resUrl = "${res.fhirType()}/${res.idElement?.idPart.orEmpty()}"
        return BundleEntryComponent().apply {
            resource = res
            fullUrl = resUrl
            request = Bundle.BundleEntryRequestComponent().apply {
                method = Bundle.HTTPVerb.PUT
                url = resUrl
            }
        }
    }

    private suspend fun uploadToFhirServer(files: List<File>, batchSize: Int = 10) {
        val bundles = mutableListOf<BundleEntryComponent>()
        for (file in files) {
            try {
                if (file.extension == "map") {
                    val map = compileMapToJson(file) ?: continue
                    bundles.add(createBundleEntry(map))
                } else {
                    val quest = compileQuestionnaire(file) ?: continue
                    bundles.add(createBundleEntry(quest))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        uploader.bundleUpload(bundles, batchSize)
    }

    private fun processExcludedPaths(projectRoot: String) {
        for (exclude in projectConfig.uploadExclude) {
            excludeList.add(Paths.get(projectRoot, exclude).normalize().toAbsolutePath().toString())
        }
    }

    private fun processFiles(directory: File, extension: String) {
        val files = directory.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory) {
                val isNotExcluded = !excludeList.contains(file.absolutePath)
                if (isNotExcluded) {
                    processFiles(file, extension)
                }
            } else {
                if (extension == file.extension) {
                    uploadList.add(file)
                }
            }
        }
    }
}