package net.cakemc.minimix.wrapper

import net.cakemc.minimix.MinimixClassLoader
import net.cakemc.minimix.dependency.Repository

object LaunchWrapper {

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size < 1) {
            println("Usage: LaunchWrapper <main-class> [args...]")
            return
        }

        // Config (could be loaded from file instead)
        val mainClassName = args[0]
        val appArgs = args.drop(1).toTypedArray()

        val repositories = listOf(
            Repository("https://repo.maven.apache.org/maven2"),
            Repository("https://private.repo.com/repo", "user", "password")
        )

        val dependencies = listOf(
            "org.jetbrains.kotlin:kotlin-stdlib:1.9.0",
            // Add more deps as needed
        )

        val minimixs = listOf(
            "com.example.MyMiniMix",
        )

        val minimixLoader = MinimixClassLoader(minimixs, dependencies, repositories)
        Thread.currentThread().contextClassLoader = minimixLoader

        val mainClass = minimixLoader.loadClass(mainClassName)
        val mainMethod = mainClass.getMethod("main", Array<String>::class.java)
        mainMethod.invoke(null, appArgs)
    }
}
