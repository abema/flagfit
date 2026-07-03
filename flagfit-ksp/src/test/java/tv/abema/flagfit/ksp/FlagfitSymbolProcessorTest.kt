package tv.abema.flagfit.ksp

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspSourcesDir
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.abema.flagfit.DebugAnnotationAdapter
import tv.abema.flagfit.Flagfit
import tv.abema.flagfit.FlagType
import tv.abema.flagfit.JustFlagSource
import java.io.File
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

@OptIn(ExperimentalCompilerApi::class)
class FlagfitSymbolProcessorTest {

  private lateinit var compilation: KotlinCompilation

  private fun compile(vararg sources: SourceFile): JvmCompilationResult {
    compilation = KotlinCompilation().apply {
      this.sources = sources.toList()
      configureKsp(useKsp2 = true) {
        symbolProcessorProviders += FlagfitProcessorProvider()
      }
      inheritClassPath = true
      messageOutputStream = System.out
    }
    return compilation.compile()
  }

  private fun generatedFile(name: String): File {
    return compilation.kspSourcesDir.walkTopDown().first { it.name == name }
  }

  private fun invokeSuspendFunction(instance: Any, methodName: String): Any? {
    return runBlocking {
      suspendCoroutineUninterceptedOrReturn { continuation ->
        instance.javaClass
          .getMethod(methodName, Continuation::class.java)
          .invoke(instance, continuation)
      }
    }
  }

  @Test
  fun generatesImplAndCreateExtensionForFlagService() {
    val result = compile(BLOCKING_SERVICE_SOURCE)

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    val generated = generatedFile("TestFlagServiceImpl.kt").readText()
    assertTrue(generated.contains("public class TestFlagServiceImpl("))
    assertTrue(generated.contains("private val flagfit: Flagfit,"))
    assertTrue(generated.contains(") : TestFlagService {"))
    assertTrue(generated.contains("flagfit.resolveBooleanFlag("))
    assertTrue(generated.contains("flagfit.resolveVariationFlag("))
    assertTrue(generated.contains("variationType = XYZ::class"))
    assertTrue(generated.contains("DebugWith(value = JustFlagSource.True::class)"))
    assertTrue(
      generated.contains("public fun Flagfit.createTestFlagService(): TestFlagService")
    )
  }

  @Test
  fun generatedBlockingServiceReturnsFlagValues() {
    val result = compile(BLOCKING_SERVICE_SOURCE)

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    val flagfit = Flagfit(
      flagSources = listOf(JustFlagSource.StringSource("Z")),
      baseEnv = mapOf(Flagfit.ENV_IS_DEBUG_KEY to true),
      variationAdapters = listOf(XYZ),
      annotationAdapters = listOf(DebugAnnotationAdapter())
    )
    val implClass = result.classLoader.loadClass("com.example.TestFlagServiceImpl")
    val service = implClass.getConstructor(Flagfit::class.java).newInstance(flagfit)

    assertEquals(true, implClass.getMethod("defaultOnly").invoke(service))
    assertEquals(true, implClass.getMethod("debugTrue").invoke(service))
    assertEquals(XYZ.Z, implClass.getMethod("variation").invoke(service))
  }

  @Test
  fun generatedSuspendServiceWorksWithoutSuspendReturnType() {
    val result = compile(SUSPENDABLE_SERVICE_SOURCE)

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    val flagfit = Flagfit(
      flagSources = listOf(FakeSuspendableBooleanFlagSource, JustFlagSource.StringSource("X")),
      baseEnv = mapOf(Flagfit.ENV_IS_DEBUG_KEY to true),
      variationAdapters = listOf(XYZ),
      annotationAdapters = listOf(DebugAnnotationAdapter())
    )
    val implClass = result.classLoader.loadClass("com.example.SuspendFlagServiceImpl")
    val service = implClass.getConstructor(Flagfit::class.java).newInstance(flagfit)

    assertEquals(true, invokeSuspendFunction(service, "remoteEnabled"))
    assertEquals(XYZ.X, invokeSuspendFunction(service, "variation"))
  }

  @Test
  fun flagTypeAnnotationsArePassedThroughToAnnotationAdapters() {
    val source = SourceFile.kotlin(
      "OpsFlagService.kt",
      """
      package com.example

      import tv.abema.flagfit.FlagType
      import tv.abema.flagfit.annotation.BooleanFlag

      interface OpsFlagService {
        @BooleanFlag(key = "ops-enabled", defaultValue = false)
        @FlagType.Ops(owner = "Hoge Fuga", expiryDate = FlagType.EXPIRY_DATE_INFINITE)
        fun opsEnabled(): Boolean
      }
      """.trimIndent()
    )

    val result = compile(source)

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    // Normalizes whitespaces because KotlinPoet wraps long lines
    val generated = generatedFile("OpsFlagServiceImpl.kt").readText()
      .replace(Regex("\\s+"), " ")
    assertTrue(
      generated.contains("""FlagType.Ops(owner = "Hoge Fuga", expiryDate = "EXPIRY_DATE_INFINITE")""")
    )

    val flagfit = Flagfit(
      flagSources = listOf(FakeOpsFlagSource),
      annotationAdapters = FlagType.annotationAdapters()
    )
    val implClass = result.classLoader.loadClass("com.example.OpsFlagServiceImpl")
    val service = implClass.getConstructor(Flagfit::class.java).newInstance(flagfit)

    assertEquals(true, implClass.getMethod("opsEnabled").invoke(service))
  }

  @Test
  fun reportsErrorWhenFlagFunctionIsDeclaredInClass() {
    val source = SourceFile.kotlin(
      "NotInterface.kt",
      """
      package com.example

      import tv.abema.flagfit.annotation.BooleanFlag

      class NotInterface {
        @BooleanFlag(key = "key", defaultValue = false)
        fun flag(): Boolean = false
      }
      """.trimIndent()
    )

    val result = compile(source)

    assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
    assertTrue(result.messages.contains("must be declared in an interface"))
  }

  @Test
  fun reportsErrorWhenFlagFunctionHasParameters() {
    val source = SourceFile.kotlin(
      "HasParameter.kt",
      """
      package com.example

      import tv.abema.flagfit.annotation.BooleanFlag

      interface HasParameter {
        @BooleanFlag(key = "key", defaultValue = false)
        fun flag(userId: String): Boolean
      }
      """.trimIndent()
    )

    val result = compile(source)

    assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
    assertTrue(result.messages.contains("Flag functions must not have parameters"))
  }

  @Test
  fun reportsErrorWhenBooleanFlagFunctionDoesNotReturnBoolean() {
    val source = SourceFile.kotlin(
      "WrongReturnType.kt",
      """
      package com.example

      import tv.abema.flagfit.annotation.BooleanFlag

      interface WrongReturnType {
        @BooleanFlag(key = "key", defaultValue = false)
        fun flag(): String
      }
      """.trimIndent()
    )

    val result = compile(source)

    assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
    assertTrue(result.messages.contains("@BooleanFlag functions must return Boolean"))
  }

  @Test
  fun reportsErrorWhenAbstractFunctionHasNoFlagAnnotation() {
    val source = SourceFile.kotlin(
      "MissingAnnotation.kt",
      """
      package com.example

      import tv.abema.flagfit.annotation.BooleanFlag

      interface MissingAnnotation {
        @BooleanFlag(key = "key", defaultValue = false)
        fun flag(): Boolean

        fun notAFlag(): Boolean
      }
      """.trimIndent()
    )

    val result = compile(source)

    assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
    assertTrue(
      result.messages.contains("@BooleanFlag or @VariationFlag annotation is required")
    )
  }

  @Test
  fun reportsErrorWhenBothFlagAnnotationsAreDeclared() {
    val source = SourceFile.kotlin(
      "BothAnnotations.kt",
      """
      package com.example

      import tv.abema.flagfit.annotation.BooleanFlag
      import tv.abema.flagfit.annotation.VariationFlag

      interface BothAnnotations {
        @BooleanFlag(key = "key", defaultValue = false)
        @VariationFlag(key = "key", defaultValue = "A")
        fun flag(): Boolean
      }
      """.trimIndent()
    )

    val result = compile(source)

    assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
    assertTrue(
      result.messages.contains(
        "@BooleanFlag and @VariationFlag must not be declared on the same function"
      )
    )
  }

  companion object {
    private val BLOCKING_SERVICE_SOURCE = SourceFile.kotlin(
      "TestFlagService.kt",
      """
      package com.example

      import tv.abema.flagfit.JustFlagSource
      import tv.abema.flagfit.annotation.BooleanFlag
      import tv.abema.flagfit.annotation.DebugWith
      import tv.abema.flagfit.annotation.VariationFlag
      import tv.abema.flagfit.ksp.XYZ

      interface TestFlagService {
        @BooleanFlag(key = "default-only", defaultValue = true)
        fun defaultOnly(): Boolean

        @BooleanFlag(key = "debug-true", defaultValue = false)
        @DebugWith(JustFlagSource.True::class)
        fun debugTrue(): Boolean

        @VariationFlag(key = "variation", defaultValue = "Y")
        @DebugWith(JustFlagSource.StringSource::class)
        fun variation(): XYZ
      }
      """.trimIndent()
    )

    private val SUSPENDABLE_SERVICE_SOURCE = SourceFile.kotlin(
      "SuspendFlagService.kt",
      """
      package com.example

      import tv.abema.flagfit.JustFlagSource
      import tv.abema.flagfit.annotation.BooleanFlag
      import tv.abema.flagfit.annotation.DebugWith
      import tv.abema.flagfit.annotation.VariationFlag
      import tv.abema.flagfit.ksp.FakeSuspendableBooleanFlagSource
      import tv.abema.flagfit.ksp.XYZ

      interface SuspendFlagService {
        @BooleanFlag(key = "remote-enabled", defaultValue = false)
        @DebugWith(FakeSuspendableBooleanFlagSource::class)
        suspend fun remoteEnabled(): Boolean

        @VariationFlag(key = "variation", defaultValue = "Y")
        @DebugWith(JustFlagSource.StringSource::class)
        suspend fun variation(): XYZ
      }
      """.trimIndent()
    )
  }
}
