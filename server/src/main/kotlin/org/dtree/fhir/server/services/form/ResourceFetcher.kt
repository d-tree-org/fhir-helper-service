package org.dtree.fhir.server.services.form

import ca.uhn.fhir.parser.IParser
import io.github.cdimascio.dotenv.Dotenv
import org.dtree.fhir.core.compiler.parsing.ParseJsonCommands
import org.dtree.fhir.core.config.ProjectConfig
import org.dtree.fhir.core.config.ProjectConfigManager
import org.dtree.fhir.core.di.FhirProvider
import org.dtree.fhir.core.utils.readFile
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.transport.TransportHttp
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.StructureMap
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.pathString


interface ResourceFetcher {
    fun fetchStructureMap(id: String): StructureMap
    fun getResponseTemplate(s: String): String
    fun getQuestionnaire(s: String): Questionnaire
}

class LocalResourceFetcher(private val dotEnv: Dotenv, private val iParser: IParser, val fhirProvider: FhirProvider) :
    ResourceFetcher {

    private val baseDir: Path = Path.of(dotEnv["RESOURCES_CACHE_DIR"] ?: "repo-cache")
    private val cacheExpiryHours: Long = 24
    private var projectConfig: ProjectConfig = ProjectConfig()
    private val credentialsProvider: UsernamePasswordCredentialsProvider
    private val transportConfigCallback: TransportConfigCallback

    init {
        Files.createDirectories(baseDir)
        credentialsProvider = UsernamePasswordCredentialsProvider(dotEnv["RESOURCES_GIT_KEY"], "")
        transportConfigCallback = TransportConfigCallback { transport ->
            if (transport is TransportHttp) {
                transport.additionalHeaders =
                    mapOf(Pair("Authorization", "Bearer ${dotEnv["RESOURCES_GIT_KEY"]}"))
            }
        }
    }

    private fun repoPath(): Path {
        val repoUrl = dotEnv["RESOURCES_CACHE_URL"] ?: ""
        val repoName = extractRepoName(repoUrl)
        return baseDir.resolve(repoName)
    }

    fun getRepository(): Path {
        val repoUrl = dotEnv["RESOURCES_CACHE_URL"] ?: ""
        val repoCachePath = repoPath()

        if (shouldUpdateCache(repoCachePath)) {
            synchronized(this) {
                cloneOrUpdateRepo(repoUrl, repoCachePath.toFile())
            }
        }

        return repoCachePath
    }

    private fun extractRepoName(repoUrl: String): String {
        return repoUrl.substringAfterLast("/")
            .removeSuffix(".git")
    }

    private fun shouldUpdateCache(repoCachePath: Path): Boolean {
        if (!repoCachePath.exists()) return true

        val lastModified = repoCachePath.getLastModifiedTime().toInstant()
        val expiryThreshold = Instant.now().minusSeconds(cacheExpiryHours * 3600)

        return lastModified.isBefore(expiryThreshold)
    }

    private fun updateExistingRepo(repoDir: File) {
        try {
            Git.open(repoDir).use { git ->
                git.reset().setMode(ResetCommand.ResetType.HARD).call()
                val call = git
                    .pull()
                    .setCredentialsProvider(credentialsProvider)
                    .setTransportConfigCallback(transportConfigCallback)
                    .call()
                call
            }
        } catch (e: Exception) {
            throw GitHubRepoCacheException("Failed to update repository", e)
        }
    }

    private fun cloneNewRepo(repoUrl: String, repoDir: File) {
        try {
            Git.cloneRepository()
                .setURI(repoUrl)
                .setDepth(1)
                .setBranch(dotEnv["RESOURCES_CACHE_TAG"] ?: "production")
                .setDirectory(repoDir)
                .setCredentialsProvider(credentialsProvider)
                .setTransportConfigCallback(transportConfigCallback)
                .call()
                .close()
        } catch (e: Exception) {
            throw GitHubRepoCacheException("Failed to clone repository", e)
        }
    }

    private fun cloneOrUpdateRepo(repoUrl: String, repoDir: File) {
        if (repoDir.exists()) {
            updateExistingRepo(repoDir)
        } else {
            cloneNewRepo(repoUrl, repoDir)
        }
        projectConfig = ProjectConfigManager().loadProjectConfig(
            projectRoot = repoPath().pathString,
            file = null
        )
    }

    override fun fetchStructureMap(id: String): StructureMap {
        return ParseJsonCommands().parseStructureMap(
            repoPath().resolve(id).pathString,
            fhirProvider.parser,
            fhirProvider.scu(), projectConfig
        ) ?: throw Exception("Failed to fetch StructureMap")
    }

    override fun getResponseTemplate(s: String): String {
        return repoPath().resolve("response-templates/${s}.mustache").toFile().readFile()
    }

    override fun getQuestionnaire(s: String): Questionnaire {
        val questStr = repoPath().resolve(s).toFile().readFile()
        return iParser.parseResource(Questionnaire::class.java, questStr)
    }
}

class GitHubRepoCacheException(message: String, cause: Throwable? = null) :
    Exception(message, cause)