package net.cakemc.minimix.dependency

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.util.*

/**
 * Handles resolving, parsing, and downloading of Maven-style dependencies from one or more repositories.
 *
 * This class supports authenticated and unauthenticated repositories, and downloads dependencies
 * as temporary `.jar` files.
 *
 * @property repositories A list of [Repository] objects to try for dependency resolution.
 */
class DependencyManager(private val repositories: List<Repository>) {

    /**
     * Parses a dependency string into a [Dependency] object.
     *
     * Accepted formats:
     * - "group:name:version"
     * - "group:name:version:classifier"
     *
     * @param depString The string to parse (e.g., `"org.example:mylib:1.0.0"`).
     * @return A [Dependency] representing the parsed coordinates.
     * @throws IllegalArgumentException If the string does not conform to valid formats.
     */
    fun parseDependency(depString: String): Dependency {
        val parts = depString.split(":")
        return when (parts.size) {
            3 -> Dependency(parts[0], parts[1], parts[2])
            4 -> Dependency(parts[0], parts[1], parts[2], parts[3])
            else -> throw IllegalArgumentException("Invalid dependency format: $depString")
        }
    }

    /**
     * Attempts to download a JAR file for the given dependency from the configured repositories.
     *
     * Tries each repository in order until the JAR is successfully downloaded.
     *
     * @param dep The dependency to download.
     * @return A [File] pointing to the downloaded temporary JAR file.
     * @throws RuntimeException If the download fails from all repositories.
     */
    fun downloadJar(dep: Dependency): File {
        val exceptions = mutableListOf<Exception>()

        for (repo in repositories) {
            try {
                val url = buildUrl(repo, dep)
                val connection = createConnection(url, repo)
                return downloadFile(connection)
            } catch (ex: Exception) {
                exceptions.add(ex)
            }
        }

        throw RuntimeException("Failed to download ${dep.name}:${dep.version} from all repositories.\nErrors: ${exceptions.joinToString("\n")}")
    }

    /**
     * Constructs a full URL to the JAR file based on the repository and dependency information.
     *
     * @param repo The repository from which to download.
     * @param dep The dependency to construct the path for.
     * @return A string representing the full URL to the dependency JAR.
     */
    private fun buildUrl(repo: Repository, dep: Dependency): String {
        val basePath = "${dep.group.replace('.', '/')}/${dep.name}/${dep.version}"
        val fileName = buildString {
            append("${dep.name}-${dep.version}")
            dep.classifier?.let { append("-$it") }
            append(".jar")
        }
        return "${repo.url.trimEnd('/')}/$basePath/$fileName"
    }

    /**
     * Creates an HTTP connection to the specified URL, adding authentication headers if needed.
     *
     * @param url The full URL to connect to.
     * @param repo The repository information (may contain credentials).
     * @return An open [HttpURLConnection] ready for use.
     */
    private fun createConnection(url: String, repo: Repository): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        if (!repo.username.isNullOrBlank() && !repo.password.isNullOrBlank()) {
            val auth = "${repo.username}:${repo.password}"
            val encoded = Base64.getEncoder().encodeToString(auth.toByteArray())
            conn.setRequestProperty("Authorization", "Basic $encoded")
        }
        conn.requestMethod = "GET"
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.instanceFollowRedirects = true
        return conn
    }

    /**
     * Downloads the content from an open HTTP connection and stores it in a temporary file.
     *
     * @param connection The [HttpURLConnection] to read from.
     * @return A [File] pointing to the temporary downloaded JAR.
     * @throws IOException If the HTTP response is not successful or the file cannot be written.
     */
    private fun downloadFile(connection: HttpURLConnection): File {
        if (connection.responseCode != 200) {
            throw IOException("HTTP ${connection.responseCode} - ${connection.responseMessage}")
        }

        val tempFile = Files.createTempFile("dep-", ".jar").toFile().apply { deleteOnExit() }
        connection.inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }
}
