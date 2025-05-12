package net.cakemc.minimix.dependency

/**
 * Represents a Maven dependency coordinate.
 *
 * This is used to identify and download a specific artifact (JAR) from a repository.
 *
 * @property group The group ID of the dependency (e.g., "org.jetbrains.kotlin").
 * @property name The artifact ID or name of the dependency (e.g., "kotlin-stdlib").
 * @property version The version of the dependency (e.g., "1.8.0").
 * @property classifier Optional classifier for the dependency (e.g., "sources", "javadoc").
 */
data class Dependency(
    val group: String,
    val name: String,
    val version: String,
    val classifier: String? = null
)
