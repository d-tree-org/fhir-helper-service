package org.dtree.fhir.server.plugins

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import org.dtree.fhir.server.util.loadEnv

val dotenv = loadEnv()

fun Application.configureSecurity() {
    authentication {
        oauth("keycloakOAuth") {
            client = HttpClient(Apache)
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "keycloak",
                    authorizeUrl = dotenv["KEYCLOAK_AUTHORIZE_TOKEN_URL"],
                    accessTokenUrl = dotenv["KEYCLOAK_ACCESS_TOKEN_URL"],
                    clientId = dotenv["KEYCLOAK_ID"],
                    clientSecret = dotenv["KEYCLOAK_SECRET"],
                    accessTokenRequiresBasicAuth = false,
                    requestMethod = HttpMethod.Post,
                    defaultScopes = listOf()
                )
            }
            urlProvider = {
                "http://127.0.0.1:4040/callback"
            }
        }
    }
}