package tv.abema.flagfit

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tv.abema.flagfit.DevelopAnnotationAdapter.Companion.ENV_IS_DEVELOP_KEY
import tv.abema.flagfit.JustFlagSource.False
import tv.abema.flagfit.JustFlagSource.True
import tv.abema.flagfit.annotation.BooleanEnv
import tv.abema.flagfit.annotation.BooleanFlag
import tv.abema.flagfit.annotation.DebugWith
import tv.abema.flagfit.annotation.ReleaseWith
import tv.abema.flagfit.annotation.VariationFlag
import kotlin.reflect.KClass
import kotlin.reflect.KFunction1
import kotlin.reflect.KSuspendFunction1

@RunWith(Enclosed::class)
class FlagfitTest {
  @RunWith(Parameterized::class)
  class BlockingFlagfit(val param: Param) {
    class Param(
      val booleanFlagSource: BlockingBooleanFlagSource,
      val function: KFunction1<BlockingService, Boolean>,
      val env: Map<String, Any>,
      val customAnnotationAdapters: List<AnnotationAdapter<out Annotation>> = listOf(),
      val expected: Boolean,
    )

    companion object {
      @JvmStatic
      @Parameterized.Parameters(name = "{0}")
      fun data(): List<Param> {
        val shouldNotCalledRemoteBlockingSource = object : RemoteBlockingBooleanFlagSource {
          override fun get(key: String, defaultValue: Boolean, env: Map<String, Any>): Boolean {
            error("should not be called")
          }
        }
        return listOf(
          Param(
            booleanFlagSource = shouldNotCalledRemoteBlockingSource,
            function = BlockingService::defaultOnly,
            env = mapOf(Flagfit.ENV_IS_DEBUG_KEY to true),
            expected = true
          ),
          Param(
            booleanFlagSource = shouldNotCalledRemoteBlockingSource,
            function = BlockingService::defaultOnly,
            env = mapOf(Flagfit.ENV_IS_DEBUG_KEY to false),
            expected = true
          ),
          Param(
            booleanFlagSource = shouldNotCalledRemoteBlockingSource,
            function = BlockingService::releaseRemote,
            env = mapOf(Flagfit.ENV_IS_DEBUG_KEY to true),
            expected = false
          ),
          Param(
            booleanFlagSource = object : RemoteBlockingBooleanFlagSource {
              override fun get(key: String, defaultValue: Boolean, env: Map<String, Any>): Boolean {
                return true
              }
            },
            function = BlockingService::releaseRemote,
            env = mapOf(Flagfit.ENV_IS_DEBUG_KEY to false),
            expected = true
          ),
          Param(
            booleanFlagSource = shouldNotCalledRemoteBlockingSource,
            function = BlockingService::developDebug,
            env = mapOf(
              Flagfit.ENV_IS_DEBUG_KEY to true,
              ENV_IS_DEVELOP_KEY to false
            ),
            customAnnotationAdapters = listOf(DevelopAnnotationAdapter()),
            expected = false
          ),
          Param(
            booleanFlagSource = shouldNotCalledRemoteBlockingSource,
            function = BlockingService::developDebug,
            env = mapOf(
              Flagfit.ENV_IS_DEBUG_KEY to false,
              ENV_IS_DEVELOP_KEY to true
            ),
            customAnnotationAdapters = listOf(DevelopAnnotationAdapter()),
            expected = true
          ),
          Param(
            booleanFlagSource = object : RemoteBlockingBooleanFlagSource {
              override fun get(key: String, defaultValue: Boolean, env: Map<String, Any>): Boolean {
                assert(env[ENV_IS_DEVELOP_KEY] as Boolean)
                return true
              }
            },
            function = BlockingService::forceDevelopFlag,
            env = mapOf(
              Flagfit.ENV_IS_DEBUG_KEY to false,
              ENV_IS_DEVELOP_KEY to false
            ),
            customAnnotationAdapters = listOf(DevelopAnnotationAdapter()),
            expected = true
          )
        )
      }
    }

    @Test
    fun test() {
      val flagfit = Flagfit(
        flagSources = listOf(param.booleanFlagSource, JustFlagSource.StringSource("C")),
        baseEnv = param.env,
        variationAdapters = listOf(ABC),
        annotationAdapters = listOf(
          DevelopAnnotationAdapter(),
          ReleaseAnnotationAdapter(),
          DebugAnnotationAdapter()
        ) + param.customAnnotationAdapters
      )
      val service = flagfit.create(BlockingService::class)

      val value = param.function.invoke(service)

      check(param.expected == value) {
        "should be ${param.expected} but actual $value\n" +
          "flagState:" + flagfit.getFlagStates(BlockingService::class)
          .first { it.method.name == param.function.name }
      }
    }
  }

  @RunWith(Parameterized::class)
  class VariationBlockingFlagfit(val param: Param<*>) {
    class Param<T>(
      val stringFlagSource: BlockingStringFlagSource,
      val function: KFunction1<BlockingService, T>,
      val env: Map<String, Any>,
      val customAnnotationAdapters: List<AnnotationAdapter<out Annotation>> = listOf(),
      val expected: T,
    )

    companion object {
      @JvmStatic
      @Parameterized.Parameters(name = "{0}")
      fun data(): List<Param<*>> {
        return listOf(
          Param(
            stringFlagSource = JustFlagSource.StringSource("C"),
            function = BlockingService::variationWhenDebug,
            env = mapOf(Flagfit.ENV_IS_DEBUG_KEY to true),
            expected = ABC.C
          ),
          Param(
            stringFlagSource = JustFlagSource.StringSource("C"),
            function = BlockingService::variationWhenDebug,
            env = mapOf(Flagfit.ENV_IS_DEBUG_KEY to false),
            expected = ABC.B
          )
        )
      }
    }

    @Test
    fun test() {
      val shouldNotCalledRemoteBlockingSource = object : RemoteBlockingBooleanFlagSource {
        override fun get(key: String, defaultValue: Boolean, env: Map<String, Any>): Boolean {
          error("should not be called")
        }
      }
      val flagfit = Flagfit(
        flagSources = listOf(param.stringFlagSource, shouldNotCalledRemoteBlockingSource),
        baseEnv = param.env,
        variationAdapters = listOf(ABC),
        annotationAdapters = listOf(
          DevelopAnnotationAdapter(),
          ReleaseAnnotationAdapter(),
          DebugAnnotationAdapter()
        ) + param.customAnnotationAdapters
      )
      val service = flagfit.create(BlockingService::class)

      val value = param.function.invoke(service)

      check(param.expected == value) {
        "should be ${param.expected} but actual $value\n" +
          "flagState:" + flagfit.getFlagStates(BlockingService::class)
          .first { it.method.name == param.function.name }
      }
    }
  }

  @RunWith(Parameterized::class)
  class SuspendableFlagfit(val param: Param) {
    class Param(
      val booleanFlagSource: SuspendableBooleanFlagSource,
      val function: KSuspendFunction1<SuspendableService, Boolean>,
      val env: Map<String, Any>,
      val customAnnotationAdapters: List<AnnotationAdapter<out Annotation>> = listOf(),
      val expected: Boolean,
    )

    companion object {
      @JvmStatic
      @Parameterized.Parameters(name = "{0}")
      fun data(): List<Param> {
        return listOf(
          Param(
            booleanFlagSource = object : RemoteSuspendableBooleanFlagSource {
              override suspend fun fetch(
                key: String, defaultValue: Boolean, env: Map<String, Any>,
              ): Boolean {
                error("should not be called")
              }
            },
            function = SuspendableService::developRemoteSuspendable,
            env = mapOf(
              Flagfit.ENV_IS_DEBUG_KEY to true,
              ENV_IS_DEVELOP_KEY to false
            ),
            customAnnotationAdapters = listOf(DevelopAnnotationAdapter()),
            expected = false
          ),
          Param(
            booleanFlagSource = object : RemoteSuspendableBooleanFlagSource {
              override suspend fun fetch(
                key: String, defaultValue: Boolean, env: Map<String, Any>,
              ): Boolean {
                // for testing coroutines work
                delay(1)
                return true
              }
            },
            function = SuspendableService::developRemoteSuspendable,
            env = mapOf(
              Flagfit.ENV_IS_DEBUG_KEY to true,
              ENV_IS_DEVELOP_KEY to true
            ),
            customAnnotationAdapters = listOf(DevelopAnnotationAdapter()),
            expected = true
          ),
          Param(
            booleanFlagSource = object : RemoteSuspendableBooleanFlagSource {
              override suspend fun fetch(
                key: String, defaultValue: Boolean, env: Map<String, Any>,
              ): Boolean {
                // for testing not coroutines work
                return true
              }
            },
            function = SuspendableService::developRemoteSuspendable,
            env = mapOf(
              Flagfit.ENV_IS_DEBUG_KEY to true,
              ENV_IS_DEVELOP_KEY to true
            ),
            customAnnotationAdapters = listOf(DevelopAnnotationAdapter()),
            expected = true
          )
        )
      }
    }

    @Test
    fun test() {
      runBlocking {
        val shouldNotUsedFlagSource = object : SuspendableStringFlagSource {
          override suspend fun fetch(
            key: String, defaultValue: String, env: Map<String, Any>,
          ): String {
            throw AssertionError("Should not used this flag soruce")
          }
        }
        val flagfit = Flagfit(
          flagSources = listOf(param.booleanFlagSource, shouldNotUsedFlagSource),
          baseEnv = param.env,
          variationAdapters = listOf(ABC),
          annotationAdapters = listOf(
            DevelopAnnotationAdapter(),
            ReleaseAnnotationAdapter(),
            DebugAnnotationAdapter()
          ) + param.customAnnotationAdapters
        )
        val service = flagfit.create(SuspendableService::class)

        val value = param.function.invoke(service)

        check(param.expected == value) {
          "should be ${param.expected} but actual $value\n" +
            "flagState:" + flagfit.getFlagStates(SuspendableService::class)
            .first { it.method.name == param.function.name }
        }
      }
    }
  }

  @RunWith(Parameterized::class)
  class VariationSuspendableFlagfit(val param: Param<*>) {
    class Param<T>(
      val stringFlagSource: SuspendableStringFlagSource,
      val function: KSuspendFunction1<SuspendableService, ABC>,
      val env: Map<String, Any>,
      val customAnnotationAdapters: List<AnnotationAdapter<out Annotation>> = listOf(),
      val expected: T,
    )

    companion object {
      @JvmStatic
      @Parameterized.Parameters(name = "{0}")
      fun data(): List<Param<*>> {
        return listOf(
          Param(
            stringFlagSource = JustFlagSource.StringSource("C"),
            function = SuspendableService::variationWidthDevelopSuspendable,
            env = mapOf(ENV_IS_DEVELOP_KEY to true),
            expected = ABC.C
          ),
          Param(
            stringFlagSource = JustFlagSource.StringSource("C"),
            function = SuspendableService::variationWidthDevelopSuspendable,
            env = mapOf(Flagfit.ENV_IS_DEBUG_KEY to false),
            expected = ABC.B
          )
        )
      }
    }

    @Test
    fun test() {
      val shouldNotCalledRemoteSuspendableSource = object : RemoteSuspendableBooleanFlagSource {
        override suspend fun fetch(
          key: String, defaultValue: Boolean, env: Map<String, Any>,
        ): Boolean {
          error("should not be called")
        }
      }
      runBlocking {
        val flagfit = Flagfit(
          flagSources = listOf(param.stringFlagSource, shouldNotCalledRemoteSuspendableSource),
          baseEnv = param.env,
          variationAdapters = listOf(ABC),
          annotationAdapters = listOf(
            DevelopAnnotationAdapter(),
            ReleaseAnnotationAdapter(),
            DebugAnnotationAdapter()
          ) + param.customAnnotationAdapters
        )
        val service = flagfit.create(SuspendableService::class)

        val value = param.function.invoke(service)

        check(param.expected == value) {
          "should be ${param.expected} but actual $value\n" +
            "flagState:" + flagfit.getFlagStates(SuspendableService::class)
            .first { it.method.name == param.function.name }
        }
      }
    }
  }
}

interface RemoteBlockingBooleanFlagSource : BlockingBooleanFlagSource
interface RemoteSuspendableBooleanFlagSource : SuspendableBooleanFlagSource

interface BlockingService {
  @BooleanFlag(
    key = "defaultOnly",
    defaultValue = true
  )
  fun defaultOnly(): Boolean

  @BooleanFlag(
    key = "releaseRemote",
    defaultValue = false
  )
  @ReleaseWith(RemoteBlockingBooleanFlagSource::class)
  fun releaseRemote(): Boolean

  @BooleanFlag(
    key = "developTrue",
    defaultValue = false
  )
  @DevelopWith(True::class)
  @DebugWith(False::class)
  fun developDebug(): Boolean

  @BooleanFlag(
    key = "releaseRemote",
    defaultValue = false
  )
  @DevelopWith(BlockingBooleanFlagSource::class)
  @BooleanEnv(key = ENV_IS_DEVELOP_KEY, value = true)
  fun forceDevelopFlag(): Boolean

  @VariationFlag(key = "my-key", defaultValue = "B")
  @DebugWith(JustFlagSource.StringSource::class)
  fun variationWhenDebug(): ABC
}

interface SuspendableService {
  @BooleanFlag(
    key = "developRemoteSuspendable",
    defaultValue = false
  )
  @DevelopWith(RemoteSuspendableBooleanFlagSource::class)
  @ReleaseWith(False::class)
  suspend fun developRemoteSuspendable(): Boolean

  @VariationFlag(
    key = "variationWidthDevelopSuspendable",
    defaultValue = "B"
  )
  @DevelopWith(SuspendableStringFlagSource::class)
  @SuspendReturnType(ABC::class)
  suspend fun variationWidthDevelopSuspendable(): ABC
}

annotation class DevelopWith(
  val flagSourceClass: KClass<out FlagSource>,
)

class DevelopAnnotationAdapter : AnnotationAdapter<DevelopWith> {
  override fun canHandle(
    annotation: DevelopWith,
    env: Map<String, Any>,
  ): Boolean {
    return env[ENV_IS_DEVELOP_KEY] == true
  }

  override fun flagSourceClass(annotation: DevelopWith): KClass<out FlagSource> {
    return annotation.flagSourceClass
  }

  override fun annotationClass(): KClass<DevelopWith> {
    return DevelopWith::class
  }

  override fun flagMetaData(annotation: DevelopWith): FlagMetadata {
    return FlagMetadata(
      author = "",
      description = "",
      expiryDate = null,
      nowDate = null
    )
  }

  companion object {
    const val ENV_IS_DEVELOP_KEY = "ENV_IS_DEVELOP_KEY"
  }
}

enum class ABC {
  A, B, C;

  companion object : VariationAdapter<ABC>(ABC::class) {
    override fun variationOf(value: String): ABC {
      return values().firstOrNull { it.name == value } ?: A
    }
  }
}

