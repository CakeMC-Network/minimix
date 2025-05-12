package net.cakemc.minimix

import net.cakemc.minimix.dependency.DependencyManager
import net.cakemc.minimix.dependency.Repository
import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import java.io.File
import java.io.InputStream
import java.lang.reflect.Method
import java.net.URLClassLoader

/**
 * A custom class loader capable of applying mixin transformations to Java classes at load time.
 *
 * Mixins allow modification of existing classes without directly modifying their source code.
 * This class loader supports:
 * - Adding methods and fields from mixins.
 * - Replacing or injecting code into existing methods using annotations.
 * - Shadowing fields for field redirection.
 *
 * @property mixinClassNames The fully-qualified class names of mixin classes to load and apply.
 * @property dependencies A list of dependencies in string format to be resolved and loaded.
 * @property repositories Repositories used by [DependencyManager] to resolve dependencies.
 * @constructor Loads dependencies, reads mixins, and prepares them for transformation.
 */
class MixinClassLoader(
    private val mixinClassNames: List<String>,
    private val dependencies: List<String> = listOf(),
    private val repositories: List<Repository>,
    parent: ClassLoader = getSystemClassLoader()
) : ClassLoader(parent) {

    /** Map of target class names to their associated mixin nodes and class references */
    private val mixins: Map<String, List<Pair<ClassNode, Class<*>>>>

    /** Handles parsing and downloading of dependencies */
    private val dependencyManager = DependencyManager(repositories)

    /** List of loaded JAR files added to the classpath */
    private val loadedJars = mutableListOf<File>()

    init {
        for (depString in dependencies) {
            val dep = dependencyManager.parseDependency(depString)
            val jarFile = dependencyManager.downloadJar(dep)
            loadedJars.add(jarFile)
            addJarToClasspath(jarFile)
        }

        mixins = mixinClassNames
            .mapNotNull { name ->
                val cls = parent.loadClass(name)
                val target = cls.getAnnotation(MixinTarget::class.java)?.value ?: return@mapNotNull null
                val classNode = readClassNode(name) ?: return@mapNotNull null
                target to (classNode to cls)
            }
            .groupBy({ it.first }, { it.second })
    }

    /**
     * Adds a JAR file to the parent classloader's classpath via reflection.
     *
     * @param file The JAR file to add.
     */
    private fun addJarToClasspath(file: File) {
        val method = URLClassLoader::class.java.getDeclaredMethod("addURL", java.net.URL::class.java)
        method.isAccessible = true
        method.invoke(parent, file.toURI().toURL())
    }

    /**
     * Finds and defines a class, applying mixin transformations if available.
     *
     * @param name The fully-qualified class name.
     * @return The defined class.
     */
    override fun findClass(name: String): Class<*> {
        val originalBytes = loadBytes(name)
        val transformed = mixins[name]?.let { applyMixins(name, originalBytes, it) } ?: originalBytes
        return defineClass(name, transformed, 0, transformed.size)
    }

    /**
     * Loads raw bytecode of a class by name.
     *
     * @param className The fully-qualified name.
     * @return Byte array representing the class.
     */
    private fun loadBytes(className: String): ByteArray {
        val path = className.replace('.', '/') + ".class"
        return getResourceAsStream(path)?.readBytes()
            ?: throw ClassNotFoundException("Cannot find class $className")
    }

    /**
     * Reads a class file into a [ClassNode] structure.
     *
     * @param className The fully-qualified class name.
     * @return The parsed [ClassNode] or null if the class cannot be read.
     */
    private fun readClassNode(className: String): ClassNode? {
        val path = className.replace('.', '/') + ".class"
        val stream: InputStream = getResourceAsStream(path) ?: return null
        return ClassReader(stream).let {
            val node = ClassNode()
            it.accept(node, 0)
            node
        }
    }

    /**
     * Applies all mixins to the target class.
     *
     * @param targetClassName Name of the class being transformed.
     * @param originalBytes Original class bytecode.
     * @param mixins List of mixin class nodes and classes to apply.
     * @return Transformed bytecode.
     */
    private fun applyMixins(targetClassName: String, originalBytes: ByteArray, mixins: List<Pair<ClassNode, Class<*>>>): ByteArray {
        val reader = ClassReader(originalBytes)
        val writer = ClassWriter(reader, ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
        val targetNode = ClassNode()
        reader.accept(targetNode, 0)

        val existingMethods = targetNode.methods.map { it.name + it.desc }.toMutableSet()
        val existingFields = targetNode.fields.map { it.name }.toMutableSet()

        for ((mixinNode, mixinClass) in mixins) {
            val methodMap = mixinClass.declaredMethods.associateBy { it.name }

            val shadowFields = mixinClass.declaredFields
                .filter { it.isAnnotationPresent(MixinShadow::class.java) }
                .associate {
                    val ann = it.getAnnotation(MixinShadow::class.java)
                    val shadowName = ann.name.takeIf { n -> n.isNotBlank() } ?: it.name
                    it.name to shadowName
                }

            // === Handle Interfaces ===
            val interfaces = mixinClass.interfaces.map { it.name }
            if (targetNode.interfaces.any { interfaces.contains(it) }) {
                injectMixinMethods(targetNode, mixinNode, mixinClass, methodMap, shadowFields, existingMethods, existingFields)
            }

            // === Handle Class Hierarchy ===
            val targetClassesToProcess = mutableSetOf(targetNode)
            targetNode.superName?.let { superName ->
                val superClass = findClassNodeByName(superName)
                if (superClass != null) {
                    targetClassesToProcess.add(superClass)
                }
            }

            targetClassesToProcess.forEach { classNode ->
                injectMixinMethods(classNode, mixinNode, mixinClass, methodMap, shadowFields, existingMethods, existingFields)
            }
        }

        targetNode.accept(writer)
        return writer.toByteArray()
    }

    /**
     * Injects methods and fields from a mixin into a target class node.
     */
    private fun injectMixinMethods(
        targetNode: ClassNode,
        mixinNode: ClassNode,
        mixinClass: Class<*>,
        methodMap: Map<String, Method>,
        shadowFields: Map<String, String>,
        existingMethods: MutableSet<String>,
        existingFields: MutableSet<String>
    ) {
        // === Inject fields ===
        for (field in mixinNode.fields) {
            val ann = mixinClass.declaredFields.find { it.name == field.name }?.getAnnotation(MixinField::class.java)
            val name = ann?.name?.takeIf { it.isNotBlank() } ?: field.name
            if (name !in existingFields) {
                targetNode.fields.add(FieldNode(field.access, name, field.desc, field.signature, field.value))
                existingFields += name
            }
        }

        // === Inject methods ===
        for (method in mixinNode.methods) {
            if (method.name == "<init>") continue  // Skip constructors

            val reflectMethod = methodMap[method.name] ?: continue

            // Check if the method has @MixinMethod to replace it
            val mixinAnn = reflectMethod.getAnnotation(MixinMethod::class.java)
            if (mixinAnn != null) {
                val name = mixinAnn.name.takeIf { it.isNotBlank() } ?: method.name
                val key = name + method.desc
                if (mixinAnn.replace) {
                    // Remove the existing method with the same signature
                    targetNode.methods.removeIf { it.name + it.desc == key }
                    targetNode.methods.add(cloneMethod(method, name))
                    existingMethods += key
                } else if (key !in existingMethods) {
                    targetNode.methods.add(cloneMethod(method, name))
                    existingMethods += key
                }
                continue
            }

            // Otherwise, it's an injection point
            val injectAnn = reflectMethod.getAnnotation(MixinInject::class.java) ?: continue
            val injectTarget = targetNode.methods.find {
                it.name == injectAnn.methodName && it.desc == injectAnn.methodDesc
            } ?: continue

            val remappedInsns = remapFieldAccess(
                method.instructions,
                mixinInternalName = mixinNode.name,
                targetInternalName = targetNode.name,
                shadowMap = shadowFields
            )

            when (injectAnn.location) {
                InjectLocation.HEAD -> {
                    injectTarget.instructions.insert(remappedInsns)
                }
                InjectLocation.TAIL -> {
                    injectTarget.instructions.insertBefore(injectTarget.instructions.last, remappedInsns)
                }
                InjectLocation.RETURN -> {
                    val iter = injectTarget.instructions.iterator()
                    while (iter.hasNext()) {
                        val insn = iter.next()
                        if (insn.opcode in Opcodes.IRETURN..Opcodes.RETURN) {
                            injectTarget.instructions.insertBefore(insn, cloneInsnList(remappedInsns))
                        }
                    }
                }
            }
        }
    }

    /**
     * Finds a class node among loaded mixin class nodes.
     */
    private fun findClassNodeByName(className: String): ClassNode? {
        return mixins.values.flatMap { it.map { it.first } }
            .find { it.name == className }
    }

    /**
     * Clones a [MethodNode], optionally renaming it.
     */
    private fun cloneMethod(method: MethodNode, newName: String): MethodNode {
        val clone = MethodNode(method.access, newName, method.desc, method.signature, method.exceptions?.toTypedArray())
        method.accept(clone)
        return clone
    }

    /**
     * Clones an instruction list for reuse.
     */
    private fun cloneInsnList(original: InsnList): InsnList {
        val copy = InsnList()
        val iter = original.iterator()
        while (iter.hasNext()) {
            copy.add(iter.next().clone(null))
        }
        return copy
    }

    /**
     * Rewrites field accesses in instructions from mixin to target class, applying shadow field mappings.
     */
    private fun remapFieldAccess(
        original: InsnList,
        mixinInternalName: String,
        targetInternalName: String,
        shadowMap: Map<String, String>
    ): InsnList {
        val newList = InsnList()
        val iter = original.iterator()

        while (iter.hasNext()) {
            val insn = iter.next()
            if (insn is FieldInsnNode && insn.owner == mixinInternalName) {
                val shadowName = shadowMap[insn.name]
                if (shadowName != null) {
                    newList.add(FieldInsnNode(insn.opcode, targetInternalName, shadowName, insn.desc))
                    continue
                }
            }
            newList.add(insn.clone(null))
        }

        return newList
    }
}
