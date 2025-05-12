package example

class MyTarget : Tickable {
    var count = 0

    override fun tick() {
        println("MyTarget tick: $count")
    }

    fun increaseCount() {
        count++
    }
}