package net.cakemc.minimix.dependency

/**
 * Represents a Maven-style repository used to resolve and download dependencies.
 *
 * Can optionally include authentication credentials for private repositories.
 *
 * @property url The base URL of the repository (e.g., "https://repo.maven.apache.org/maven2").
 * @property username Optional username for authentication, if required.
 * @property password Optional password for authentication, if required.
 */
data class Repository(
    val url: String,
    val username: String? = null,
    val password: String? = null
)
