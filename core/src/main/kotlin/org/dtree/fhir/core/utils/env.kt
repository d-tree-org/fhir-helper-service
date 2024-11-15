package org.dtree.fhir.core.utils

import io.github.cdimascio.dotenv.Dotenv

fun Dotenv.isDev(): Boolean {
   val env = get("ENVIRONMENT", "production")
    return env == "development"
}