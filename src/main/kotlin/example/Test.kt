package example

import net.cakemc.minimix.MixinClassLoader

// Main.kt
fun main() {
    // Initialize the MixinClassLoader with the Mixin class names to be applied
    val loader = MixinClassLoader(listOf("example.MyMixin"))

    // Dynamically load the target class
    val clazz = loader.loadClass("example.MyTarget")

    // Instantiate the target class
    val obj = clazz.getDeclaredConstructor().newInstance() as Tickable

    // Show the original behavior before any mixins
    println("=== Before Mixin ===")
    obj.tick()

    // Dynamically invoke the method that was replaced by the mixin
    println("=== After Mixin ===")
    obj.tick()  // This will use the mixin's `tick()` method

    // The mixin should have also replaced the `increaseCount()` method
    val increaseMethod = clazz.getMethod("increaseCount")
    increaseMethod.invoke(obj)
}
