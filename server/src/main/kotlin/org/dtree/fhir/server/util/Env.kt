package org.dtree.fhir.server.util

import io.github.cdimascio.dotenv.dotenv

fun loadEnv() = dotenv {
    ignoreIfMissing = true
    systemProperties = true
}