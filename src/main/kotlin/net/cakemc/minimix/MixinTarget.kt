package net.cakemc.minimix

/**
 * Specifies the target class that a mixin should be applied to.
 *
 * This annotation should be placed on a mixin class to indicate which class it should modify.
 *
 * @property value The fully qualified internal name (e.g., `com/example/MyClass`) of the target class.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class MixinTarget(val value: String)


/**
 * Marks a field in the mixin class to be injected into the target class.
 *
 * Optionally allows renaming the injected field. The field is added only if it does not already
 * exist in the target class.
 *
 * @property name Optional custom name for the injected field. If blank, the field's original name is used.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class MixinField(val name: String = "")


/**
 * Declares a method in the mixin class to be added or replace an existing method in the target class.
 *
 * If `replace` is true, any method in the target class with the same name and descriptor will be removed and
 * replaced with this one. Otherwise, the method is only added if it doesn’t exist.
 *
 * @property name Optional custom name for the method in the target class.
 * @property replace Whether to replace an existing method with the same signature.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class MixinMethod(val name: String = "", val replace: Boolean = false)


/**
 * Injects the annotated mixin method into an existing method of the target class at a specific location.
 *
 * The mixin method’s instructions will be copied and injected into the specified method at the
 * specified injection point.
 *
 * @property methodName The name of the target method in the target class.
 * @property methodDesc The descriptor (type signature) of the target method.
 * @property location The location in the method where the injection should occur (HEAD, TAIL, RETURN).
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class MixinInject(
    val methodName: String,
    val methodDesc: String,
    val location: InjectLocation
)


/**
 * Maps a field in the mixin class to an existing field in the target class.
 *
 * Used to access or modify private fields in the target class from the mixin.
 *
 * @property name The name of the field in the target class to shadow. Defaults to the field name in the mixin class.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class MixinShadow(val name: String = "")
