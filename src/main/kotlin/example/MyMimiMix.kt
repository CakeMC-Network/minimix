package example

import net.cakemc.minimix.*

// MyMiniMix.kt
@MiniMixTarget("com.example.MyTarget")
class MyMiniMix : Tickable {

    // Shadow the 'count' field from the target class
    @MiniMixShadow("count")
    var shadowedCount: Int = 0

    // Inject code at the beginning of 'tick()' method
    @MiniMixInject(methodName = "tick", methodDesc = "()V", location = InjectLocation.HEAD)
    fun beforeTick() {
        println("MiniMix before tick! Shadowed count is $shadowedCount")
    }

    // Inject code at the end of 'tick()' method
    @MiniMixInject(methodName = "tick", methodDesc = "()V", location = InjectLocation.TAIL)
    fun afterTick() {
        println("MiniMix after tick!")
    }

    // Inject code at return point of 'tick()' method
    @MiniMixInject(methodName = "tick", methodDesc = "()V", location = InjectLocation.RETURN)
    fun onReturnTick() {
        println("MiniMix on tick return!")
    }

    // Replace the 'increaseCount()' method
    @MiniMixMethod(name = "increaseCount", replace = true)
    fun customIncreaseCount() {
        println("Custom increaseCount method in MiniMix!")
    }

    // Replace the 'tick()' method (optional example)
    @MiniMixMethod(name = "tick", replace = true)
    override fun tick() {
        println("Custom tick implementation from MiniMix!")
    }
}
