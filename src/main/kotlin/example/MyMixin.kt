package example

import net.cakemc.minimix.*

// MyMixin.kt
@MixinTarget("com.example.MyTarget")
class MyMixin : Tickable {

    // Shadow the 'count' field from the target class
    @MixinShadow("count")
    var shadowedCount: Int = 0

    // Inject code at the beginning of 'tick()' method
    @MixinInject(methodName = "tick", methodDesc = "()V", location = InjectLocation.HEAD)
    fun beforeTick() {
        println("Mixin before tick! Shadowed count is $shadowedCount")
    }

    // Inject code at the end of 'tick()' method
    @MixinInject(methodName = "tick", methodDesc = "()V", location = InjectLocation.TAIL)
    fun afterTick() {
        println("Mixin after tick!")
    }

    // Inject code at return point of 'tick()' method
    @MixinInject(methodName = "tick", methodDesc = "()V", location = InjectLocation.RETURN)
    fun onReturnTick() {
        println("Mixin on tick return!")
    }

    // Replace the 'increaseCount()' method
    @MixinMethod(name = "increaseCount", replace = true)
    fun customIncreaseCount() {
        println("Custom increaseCount method in Mixin!")
    }

    // Replace the 'tick()' method (optional example)
    @MixinMethod(name = "tick", replace = true)
    override fun tick() {
        println("Custom tick implementation from Mixin!")
    }
}
