package net.cakemc.minimix

/**
 * Represents the location within a method where mixin code should be injected.
 *
 * Used in conjunction with the [MixinInject] annotation to determine where
 * in the target method the instructions from the mixin method will be inserted.
 */
enum class InjectLocation {
    /**
     * Injects at the very beginning of the target method, before any existing instructions.
     */
    HEAD,

    /**
     * Injects at the very end of the target method, right before its last instruction.
     * Note: this does not guarantee injection before every return statement.
     */
    TAIL,

    /**
     * Injects immediately before every `return` instruction in the target method.
     * This includes all return types (void, int, object, etc.).
     */
    RETURN
}
