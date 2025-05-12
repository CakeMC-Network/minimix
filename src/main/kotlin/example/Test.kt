package example

import net.cakemc.minimix.MinimixClassLoader

// Main.kt
fun main() {
    // Initialize the MiniMixClassLoader with the Minimix class names to be applied
    val loader = MinimixClassLoader(listOf("example.MyMiniMix"))

    // Dynamically load the target class
    val clazz = loader.loadClass("example.MyTarget")

    // Instantiate the target class
    val obj = clazz.getDeclaredConstructor().newInstance() as Tickable

    // Show the original behavior before any minimix
    println("=== Before ===")
    obj.tick()

    // Dynamically invoke the method that was replaced by the minimix
    println("=== After ===")
    obj.tick()  // This will use the minimix's `tick()` method

    // The minimix should have also replaced the `increaseCount()` method
    val increaseMethod = clazz.getMethod("increaseCount")
    increaseMethod.invoke(obj)
}
