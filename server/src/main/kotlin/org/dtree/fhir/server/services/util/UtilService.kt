package org.dtree.fhir.server.services.util

import ca.uhn.fhir.parser.IParser
import io.github.cdimascio.dotenv.Dotenv
import org.dtree.fhir.core.uploader.general.FailedToUploadException
import org.dtree.fhir.core.uploader.general.FhirClient
import org.dtree.fhir.core.utils.readFile
import org.hl7.fhir.r4.model.Bundle
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.walk

object UtilService : KoinComponent {
    private val dotenv by inject<Dotenv>()
    private val iParser by inject<IParser>()
    private val fhirClient by inject<FhirClient>()

    @OptIn(ExperimentalPathApi::class)
    suspend fun runCli() {
        val baseDir = dotenv["REPORT_DIR"] ?: return
        val allFiles = Path(baseDir).walk()
        allFiles.forEach { path ->
            try {
                val bundleRaw = path.toString().readFile()
                val bundle = iParser.parseResource(Bundle::class.java, bundleRaw)
                fhirClient.bundleUpload(bundle.entry, 2)
            } catch (e: Exception) {
                if (e !is FailedToUploadException) {
                    println("Something else happened, I am dying")
                    throw e
                }
                path.toFile().delete()
            }
        }
    }
}