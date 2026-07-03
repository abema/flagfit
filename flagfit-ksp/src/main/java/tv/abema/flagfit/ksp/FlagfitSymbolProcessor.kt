package tv.abema.flagfit.ksp

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

class FlagfitProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    return FlagfitSymbolProcessor(environment.codeGenerator, environment.logger)
  }
}

/**
 * Generates an implementation class for each interface that declares flag functions
 * annotated with @BooleanFlag or @VariationFlag.
 *
 * For an interface `FlagService`, `FlagServiceImpl` and a `Flagfit.createFlagService()`
 * extension function are generated so that the service can be instantiated without
 * java.lang.reflect.Proxy.
 */
class FlagfitSymbolProcessor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
) : SymbolProcessor {

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val serviceInterfaces = (
      resolver.getSymbolsWithAnnotation(BOOLEAN_FLAG_NAME) +
        resolver.getSymbolsWithAnnotation(VARIATION_FLAG_NAME)
      )
      .filterIsInstance<KSFunctionDeclaration>()
      .mapNotNull { function ->
        val parent = function.parentDeclaration
        if (parent !is KSClassDeclaration || parent.classKind != ClassKind.INTERFACE) {
          logger.error(
            "Functions annotated with @BooleanFlag or @VariationFlag " +
              "must be declared in an interface.",
            function
          )
          null
        } else {
          parent
        }
      }
      .distinctBy { it.qualifiedName?.asString() }
      .toList()

    serviceInterfaces.forEach { serviceInterface ->
      generateService(serviceInterface)
    }
    return emptyList()
  }

  private fun generateService(serviceInterface: KSClassDeclaration) {
    if (serviceInterface.typeParameters.isNotEmpty()) {
      logger.error("Flag service interfaces must not have type parameters.", serviceInterface)
      return
    }
    val abstractProperties = serviceInterface.getDeclaredProperties()
      .filter { it.isAbstract() }
      .toList()
    if (abstractProperties.isNotEmpty()) {
      abstractProperties.forEach {
        logger.error("Flag service interfaces must not have abstract properties.", it)
      }
      return
    }

    val packageName = serviceInterface.packageName.asString()
    val interfaceName = serviceInterface.toClassName()
    val baseName = interfaceName.simpleNames.joinToString("_")
    val implName = baseName + "Impl"

    val properties = mutableListOf<PropertySpec>()
    val functions = mutableListOf<FunSpec>()
    serviceInterface.getDeclaredFunctions()
      .filter { it.isAbstract }
      .forEach { function ->
        val generated = generateFlagFunction(function) ?: return
        properties += generated.first
        functions += generated.second
      }

    val visibility = if (Modifier.INTERNAL in serviceInterface.modifiers) {
      KModifier.INTERNAL
    } else {
      KModifier.PUBLIC
    }
    val implClassName = ClassName(packageName, implName)
    val typeSpec = TypeSpec.classBuilder(implClassName)
      .addModifiers(visibility)
      .addSuperinterface(interfaceName)
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addParameter("flagfit", FLAGFIT)
          .build()
      )
      .addProperty(
        PropertySpec.builder("flagfit", FLAGFIT, KModifier.PRIVATE)
          .initializer("flagfit")
          .build()
      )
      .addProperties(properties)
      .addFunctions(functions)
      .build()

    val createFunSpec = FunSpec.builder("create$baseName")
      .addModifiers(visibility)
      .receiver(FLAGFIT)
      .returns(interfaceName)
      .addStatement("return %T(this)", implClassName)
      .build()

    FileSpec.builder(packageName, implName)
      .addFileComment("Code generated by flagfit-ksp. Do not edit.")
      .addType(typeSpec)
      .addFunction(createFunSpec)
      .build()
      .writeTo(
        codeGenerator = codeGenerator,
        aggregating = false,
        originatingKSFiles = listOfNotNull(serviceInterface.containingFile)
      )
  }

  private fun generateFlagFunction(
    function: KSFunctionDeclaration,
  ): Pair<PropertySpec, FunSpec>? {
    val functionName = function.simpleName.asString()
    if (function.parameters.isNotEmpty()) {
      logger.error("Flag functions must not have parameters.", function)
      return null
    }
    if (function.typeParameters.isNotEmpty()) {
      logger.error("Flag functions must not have type parameters.", function)
      return null
    }

    val annotations = function.annotations.toList()
    val booleanFlag = annotations.firstOrNull { it.qualifiedName() == BOOLEAN_FLAG_NAME }
    val variationFlag = annotations.firstOrNull { it.qualifiedName() == VARIATION_FLAG_NAME }
    if (booleanFlag != null && variationFlag != null) {
      logger.error(
        "@BooleanFlag and @VariationFlag must not be declared on the same function.",
        function
      )
      return null
    }
    if (booleanFlag == null && variationFlag == null) {
      logger.error(
        "@BooleanFlag or @VariationFlag annotation is required for $functionName.",
        function
      )
      return null
    }

    val returnType = function.returnType?.resolve()
    if (returnType == null || returnType.isError) {
      logger.error("Could not resolve the return type of $functionName.", function)
      return null
    }
    if (returnType.isMarkedNullable) {
      logger.error("Flag functions must not return a nullable type.", function)
      return null
    }
    val returnTypeName = returnType.toTypeName()
    val isSuspend = Modifier.SUSPEND in function.modifiers
    val passThroughAnnotations = annotations.filter { it.isPassThrough() }

    val propertyName = functionName + "Resolution"
    val property: PropertySpec
    if (booleanFlag != null) {
      if (returnType.declaration.qualifiedName?.asString() != "kotlin.Boolean") {
        logger.error("@BooleanFlag functions must return Boolean.", function)
        return null
      }
      property = PropertySpec
        .builder(propertyName, BOOLEAN_FLAG_RESOLUTION, KModifier.PRIVATE)
        .delegate(
          buildCodeBlock {
            beginControlFlow("lazy")
            add("flagfit.resolveBooleanFlag(\n")
            indent()
            add("booleanFlag = %L,\n", booleanFlag.toInstantiationCode())
            add("annotations = %L,\n", passThroughAnnotations.toListCode())
            add("isSuspendFunction = %L,\n", isSuspend)
            unindent()
            add(")\n")
            endControlFlow()
          }
        )
        .build()
    } else {
      val returnDeclaration = returnType.declaration
      if (returnDeclaration !is KSClassDeclaration ||
        returnDeclaration.classKind !in listOf(ClassKind.CLASS, ClassKind.ENUM_CLASS)
      ) {
        logger.error("@VariationFlag functions must return a class or an enum class.", function)
        return null
      }
      property = PropertySpec
        .builder(
          propertyName,
          VARIATION_FLAG_RESOLUTION.parameterizedBy(returnTypeName),
          KModifier.PRIVATE
        )
        .delegate(
          buildCodeBlock {
            beginControlFlow("lazy")
            add("flagfit.resolveVariationFlag(\n")
            indent()
            add("variationFlag = %L,\n", checkNotNull(variationFlag).toInstantiationCode())
            add("variationType = %T::class,\n", returnTypeName)
            add("annotations = %L,\n", passThroughAnnotations.toListCode())
            add("isSuspendFunction = %L,\n", isSuspend)
            unindent()
            add(")\n")
            endControlFlow()
          }
        )
        .build()
    }

    val funSpec = FunSpec.builder(functionName)
      .addModifiers(KModifier.OVERRIDE)
      .apply { if (isSuspend) addModifiers(KModifier.SUSPEND) }
      .returns(returnTypeName)
      .addStatement("return %L.%L()", propertyName, if (isSuspend) "fetch" else "get")
      .build()
    return property to funSpec
  }

  private fun KSAnnotation.qualifiedName(): String? {
    return annotationType.resolve().declaration.qualifiedName?.asString()
  }

  /**
   * Annotations other than the ones consumed by the processor are reproduced on the
   * generated resolution so that AnnotationAdapters and BooleanEnv keep working.
   * Annotations from kotlin/java packages (e.g. @Deprecated) are not related to
   * flag resolution and are excluded.
   */
  private fun KSAnnotation.isPassThrough(): Boolean {
    val name = qualifiedName() ?: return false
    return name !in CONSUMED_ANNOTATION_NAMES &&
      !name.startsWith("kotlin.") &&
      !name.startsWith("java.")
  }

  private fun List<KSAnnotation>.toListCode(): CodeBlock {
    if (isEmpty()) return CodeBlock.of("emptyList()")
    return CodeBlock.of(
      "listOf(%L)",
      map { it.toInstantiationCode() }.joinToCode(", ")
    )
  }

  private fun KSAnnotation.toInstantiationCode(): CodeBlock {
    val declaration = annotationType.resolve().declaration as KSClassDeclaration
    val arguments = arguments.mapNotNull { argument ->
      val name = argument.name?.asString() ?: return@mapNotNull null
      CodeBlock.of("%L = %L", name, argument.value.toValueCode())
    }
    return CodeBlock.of("%T(%L)", declaration.toClassName(), arguments.joinToCode(", "))
  }

  private fun Any?.toValueCode(): CodeBlock {
    return when (this) {
      null -> CodeBlock.of("null")
      is String -> CodeBlock.of("%S", this)
      is Char -> CodeBlock.of("'%L'", this)
      is Boolean, is Int, is Double -> CodeBlock.of("%L", this)
      is Long -> CodeBlock.of("%LL", this)
      is Float -> CodeBlock.of("%Lf", this)
      is Byte -> CodeBlock.of("(%L).toByte()", this)
      is Short -> CodeBlock.of("(%L).toShort()", this)
      is KSType -> {
        val declaration = declaration
        if (declaration is KSClassDeclaration && declaration.classKind == ClassKind.ENUM_ENTRY) {
          CodeBlock.of("%T", declaration.toClassName())
        } else {
          CodeBlock.of("%T::class", toClassName())
        }
      }

      is KSClassDeclaration -> if (classKind == ClassKind.ENUM_ENTRY) {
        CodeBlock.of("%T", toClassName())
      } else {
        CodeBlock.of("%T::class", toClassName())
      }

      is KSAnnotation -> toInstantiationCode()
      is List<*> -> CodeBlock.of("arrayOf(%L)", map { it.toValueCode() }.joinToCode(", "))
      else -> error("Unsupported annotation value: $this (${this::class})")
    }
  }

  companion object {
    private const val BOOLEAN_FLAG_NAME = "tv.abema.flagfit.annotation.BooleanFlag"
    private const val VARIATION_FLAG_NAME = "tv.abema.flagfit.annotation.VariationFlag"
    private const val SUSPEND_RETURN_TYPE_NAME = "tv.abema.flagfit.SuspendReturnType"
    private val CONSUMED_ANNOTATION_NAMES = setOf(
      BOOLEAN_FLAG_NAME,
      VARIATION_FLAG_NAME,
      SUSPEND_RETURN_TYPE_NAME,
    )
    private val FLAGFIT = ClassName("tv.abema.flagfit", "Flagfit")
    private val BOOLEAN_FLAG_RESOLUTION =
      ClassName("tv.abema.flagfit", "BooleanFlagResolution")
    private val VARIATION_FLAG_RESOLUTION =
      ClassName("tv.abema.flagfit", "VariationFlagResolution")
  }
}
