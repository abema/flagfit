package tv.abema.flagfit

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import tv.abema.flagfit.DevelopAnnotationAdapter.Companion.ENV_IS_DEVELOP_KEY
import tv.abema.flagfit.JustFlagSource.False
import tv.abema.flagfit.JustFlagSource.True
import tv.abema.flagfit.annotation.BooleanEnv
import tv.abema.flagfit.annotation.BooleanFlag
import tv.abema.flagfit.annotation.DebugWith
import tv.abema.flagfit.annotation.VariationFlag

/**
 * Tests for the reflection-free resolution API used by flagfit-ksp generated code.
 */
class FlagResolutionTest {

  private fun flagfit(
    flagSources: List<FlagSource> = listOf(),
    env: Map<String, Any> = mapOf(),
  ): Flagfit {
    return Flagfit(
      flagSources = flagSources,
      baseEnv = env,
      variationAdapters = listOf(ABC),
      annotationAdapters = listOf(
        DevelopAnnotationAdapter(),
        ReleaseAnnotationAdapter(),
        DebugAnnotationAdapter()
      )
    )
  }

  @Test
  fun booleanFlagWithoutFlagSourceReturnsDefaultValue() {
    val resolution = flagfit().resolveBooleanFlag(
      booleanFlag = BooleanFlag(key = "default-only", defaultValue = true),
      annotations = emptyList(),
      isSuspendFunction = false
    )

    assertEquals(true, resolution.get())
  }

  @Test
  fun booleanFlagUsesFlagSourceSelectedByAnnotationAdapter() {
    val flagfit = flagfit(env = mapOf(Flagfit.ENV_IS_DEBUG_KEY to true))

    val resolution = flagfit.resolveBooleanFlag(
      booleanFlag = BooleanFlag(key = "debug-true", defaultValue = false),
      annotations = listOf(DebugWith(True::class)),
      isSuspendFunction = false
    )

    assertEquals(true, resolution.get())
  }

  @Test
  fun booleanFlagIgnoresAdapterWhichCanNotHandleEnv() {
    val flagfit = flagfit(env = mapOf(Flagfit.ENV_IS_DEBUG_KEY to false))

    val resolution = flagfit.resolveBooleanFlag(
      booleanFlag = BooleanFlag(key = "debug-true", defaultValue = false),
      annotations = listOf(DebugWith(True::class)),
      isSuspendFunction = false
    )

    assertEquals(false, resolution.get())
  }

  @Test
  fun booleanEnvAnnotationOverridesBaseEnv() {
    val flagfit = Flagfit(
      baseEnv = mapOf(ENV_IS_DEVELOP_KEY to false),
      annotationAdapters = listOf(DevelopAnnotationAdapter())
    )

    val resolution = flagfit.resolveBooleanFlag(
      booleanFlag = BooleanFlag(key = "develop-true", defaultValue = false),
      annotations = listOf(
        DevelopWith(True::class),
        BooleanEnv(key = ENV_IS_DEVELOP_KEY, value = true)
      ),
      isSuspendFunction = false
    )

    assertEquals(true, resolution.get())
  }

  @Test
  fun suspendableBooleanFlagFetchesFromSuspendableFlagSource() {
    val remoteFlagSource = object : SuspendableBooleanFlagSource {
      override suspend fun fetch(
        key: String,
        defaultValue: Boolean,
        env: Map<String, Any>,
      ): Boolean {
        delay(1)
        return true
      }
    }
    val flagfit = flagfit(
      flagSources = listOf(remoteFlagSource),
      env = mapOf(ENV_IS_DEVELOP_KEY to true)
    )

    val resolution = flagfit.resolveBooleanFlag(
      booleanFlag = BooleanFlag(key = "develop-remote", defaultValue = false),
      annotations = listOf(DevelopWith(SuspendableBooleanFlagSource::class)),
      isSuspendFunction = true
    )

    runBlocking {
      assertEquals(true, resolution.fetch())
    }
  }

  @Test
  fun variationFlagWithoutFlagSourceReturnsDefaultVariation() {
    val resolution = flagfit().resolveVariationFlag(
      variationFlag = VariationFlag(key = "abc", defaultValue = "B"),
      variationType = ABC::class,
      annotations = emptyList(),
      isSuspendFunction = false
    )

    assertEquals(ABC.B, resolution.get())
  }

  @Test
  fun variationFlagUsesFlagSourceSelectedByAnnotationAdapter() {
    val flagfit = flagfit(
      flagSources = listOf(JustFlagSource.StringSource("C")),
      env = mapOf(Flagfit.ENV_IS_DEBUG_KEY to true)
    )

    val resolution = flagfit.resolveVariationFlag(
      variationFlag = VariationFlag(key = "abc", defaultValue = "B"),
      variationType = ABC::class,
      annotations = listOf(DebugWith(JustFlagSource.StringSource::class)),
      isSuspendFunction = false
    )

    assertEquals(ABC.C, resolution.get())
  }

  @Test
  fun suspendableVariationFlagFetchesFromSuspendableFlagSource() {
    val flagfit = flagfit(
      flagSources = listOf(JustFlagSource.StringSource("A")),
      env = mapOf(Flagfit.ENV_IS_DEBUG_KEY to true)
    )

    val resolution = flagfit.resolveVariationFlag(
      variationFlag = VariationFlag(key = "abc", defaultValue = "B"),
      variationType = ABC::class,
      annotations = listOf(DebugWith(JustFlagSource.StringSource::class)),
      isSuspendFunction = true
    )

    runBlocking {
      assertEquals(ABC.A, resolution.fetch())
    }
  }

  @Test
  fun booleanFlagResolutionThrowsWhenFlagSourceIsNotRegistered() {
    val flagfit = flagfit(env = mapOf(ENV_IS_DEVELOP_KEY to true))

    assertThrows(IllegalArgumentException::class.java) {
      flagfit.resolveBooleanFlag(
        booleanFlag = BooleanFlag(key = "develop-remote", defaultValue = false),
        annotations = listOf(DevelopWith(RemoteBlockingBooleanFlagSource::class)),
        isSuspendFunction = false
      )
    }
  }

  @Test
  fun variationFlagResolutionThrowsWhenVariationAdapterIsNotRegistered() {
    val flagfit = Flagfit()

    assertThrows(IllegalStateException::class.java) {
      flagfit.resolveVariationFlag(
        variationFlag = VariationFlag(key = "abc", defaultValue = "B"),
        variationType = ABC::class,
        annotations = emptyList(),
        isSuspendFunction = false
      )
    }
  }

  @Test
  fun resolveBooleanFlagThrowsWhenSuspendFunctionUsesBlockingFlagSource() {
    val flagfit = flagfit(
      flagSources = listOf(
        object : BlockingBooleanFlagSource {
          override fun get(key: String, defaultValue: Boolean, env: Map<String, Any>): Boolean {
            return true
          }
        }
      )
    )

    assertThrows(IllegalStateException::class.java) {
      flagfit.resolveBooleanFlag(
        booleanFlag = BooleanFlag(key = "key", defaultValue = false),
        annotations = emptyList(),
        isSuspendFunction = true
      )
    }
  }

  @Test
  fun blockingGetThrowsWhenResolvedFlagSourceIsSuspendable() {
    val suspendableFlagSource = object : SuspendableBooleanFlagSource {
      override suspend fun fetch(
        key: String,
        defaultValue: Boolean,
        env: Map<String, Any>,
      ): Boolean {
        return true
      }
    }
    val flagfit = flagfit(
      flagSources = listOf(suspendableFlagSource),
      env = mapOf(ENV_IS_DEVELOP_KEY to true)
    )

    val resolution = flagfit.resolveBooleanFlag(
      booleanFlag = BooleanFlag(key = "key", defaultValue = false),
      annotations = listOf(DevelopWith(SuspendableBooleanFlagSource::class)),
      isSuspendFunction = true
    )

    assertThrows(IllegalArgumentException::class.java) {
      resolution.get()
    }
  }

  @Test
  fun resolutionMatchesDynamicProxyBehavior() {
    val flagfit = flagfit(
      flagSources = listOf(JustFlagSource.StringSource("C")),
      env = mapOf(Flagfit.ENV_IS_DEBUG_KEY to true)
    )
    val proxyService = flagfit.create(BlockingService::class)

    val defaultOnlyResolution = flagfit.resolveBooleanFlag(
      booleanFlag = BooleanFlag(key = "defaultOnly", defaultValue = true),
      annotations = emptyList(),
      isSuspendFunction = false
    )
    val variationResolution = flagfit.resolveVariationFlag(
      variationFlag = VariationFlag(key = "my-key", defaultValue = "B"),
      variationType = ABC::class,
      annotations = listOf(DebugWith(JustFlagSource.StringSource::class)),
      isSuspendFunction = false
    )

    assertEquals(proxyService.defaultOnly(), defaultOnlyResolution.get())
    assertEquals(proxyService.variationWhenDebug(), variationResolution.get())
  }
}
