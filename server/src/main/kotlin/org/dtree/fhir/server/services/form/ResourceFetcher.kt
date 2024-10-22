package org.dtree.fhir.server.services.form

import io.github.cdimascio.dotenv.Dotenv
import org.dtree.fhir.core.utils.readFile
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.sshd.IdentityPasswordProvider
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder
import org.eclipse.jgit.util.FS
import org.hl7.fhir.r4.model.StructureMap
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime


interface ResourceFetcher {
    fun fetchStructureMap(id: String): StructureMap
    fun getResponseTemplate(s: String): String
}

class LocalResourceFetcher(private val dotEnv: Dotenv) : ResourceFetcher {

    private val baseDir: Path = Path.of(dotEnv["RESOURCES_CACHE_DIR"] ?: "repo-cache")
    private val cacheExpiryHours: Long = 24

    init {
        Files.createDirectories(baseDir)
        val sshDir =
            if (!dotEnv["RESOURCES_SSH_LOCATION"].isNullOrEmpty()) File(dotEnv["RESOURCES_SSH_LOCATION"]) else File(
                FS.DETECTED.userHome(),
                "/.ssh"
            )
        val sshSessionFactory = SshdSessionFactoryBuilder().setPreferredAuthentications("publickey")
            .setHomeDirectory(FS.DETECTED.userHome()).setSshDirectory(sshDir)
            .setKeyPasswordProvider { cp: CredentialsProvider? ->
                object : IdentityPasswordProvider(cp) {
                    override fun getPassword(uri: URIish, message: String): CharArray {
                        return dotEnv["RESOURCES_SSH_PASSPHRASE"].toCharArray()
                    }
                }
            }.build(null)
        SshSessionFactory.setInstance(sshSessionFactory)
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
                git.pull().call()
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
    }

    override fun fetchStructureMap(id: String): StructureMap {
        return StructureMap()
    }

    override fun getResponseTemplate(s: String): String {
        return repoPath().resolve("response-templates/${s}.mustache").toFile().readFile()
    }
}

class GitHubRepoCacheException(message: String, cause: Throwable? = null) :
    Exception(message, cause)