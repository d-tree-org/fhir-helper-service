package org.dtree.fhir.core.tests.reports

import com.google.gson.GsonBuilder
import org.dtree.fhir.core.config.ProjectConfig
import org.dtree.fhir.core.models.TestResult
import org.dtree.fhir.core.utilities.ThrowableTypeAdapter
import org.dtree.fhir.core.utils.createFile
import org.dtree.fhir.core.utils.verifyDirectories

fun generateTestReport(result: TestResult, config: ProjectConfig) {
    val gson = GsonBuilder()
        .registerTypeAdapter(Exception::class.java, ThrowableTypeAdapter())
        .create()
    config.reportPath.verifyDirectories()
    val content = gson.toJson(MochaResults.fromTestResult(result))
    content.createFile("${config.reportPath}/report.json")
}