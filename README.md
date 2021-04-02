# Flagfit
A Flexible Flag client for Android and Kotlin

This library consists of a core **Flagfit** library and a **Flagfit flagtype** library.

## How to use

```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

```groovy
dependencies {
    // Flagfit
    implementation 'com.github.abema.flagfit:flagfit:1.0.0'
    // Flagfit flagtype
    implementation 'com.github.abema.flagfit:flagfit-flagtype:1.0.0'
}
```

## Flagfit core features

### Introduction

Flagfit turns your Feature Flags into Kotlin interface.

```kotlin
interface FlagService {
    @BooleanFlag(
      key = "new-awesome-feature",
      defaultValue = false
    )
    @DebugWith(True::class)
    @ReleaseWith(False::class)
    fun awesomeFeatureEnabled(): Boolean
}
```

The Flagfit class generates an implementation of the FlagService interface.

```kotlin
val flagfit = Flagfit(
  baseEnv = mapOf(
    ENV_IS_DEBUG_KEY to BuildConfig.DEBUG
  ),
  annotationAdapters = listOf(
    ReleaseAnnotationAdapter(),
    DebugAnnotationAdapter()
  )
)
val flagService: FlagService = flagfit.create()
```

You can use the flag by invoking it.

```kotlin
val awesomeFeatureEnabled = flagService.awesomeFeatureEnabled()
```

### Custom flag source

```kotlin
interface FlagService {
    @BooleanFlag(
      key = "new-awesome-feature",
      defaultValue = false
    )
    @DebugWith(LocalFlagSource::class)
    @ReleaseWith(False::class)
    fun awesomeFeatureEnabled(): Boolean
}
```

```kotlin
class MyLocalFlagSource @Inject constructor(
  val disk: Disk
) : LocalFlagSource {
  override fun get(
    key: String,
    defaultValue: Boolean,
    env: Map<String, Any>
  ): Boolean {
    return disk.readFlag(key, defaultValue)
  }
}

interface LocalFlagSource : BlockingBooleanFlagSource
```

```kotlin
val flagfit = Flagfit(
  flagSources = listOf(localFlagSource),
  ...
)
```

### Asynchronous flag source

You can use Kotlin Coroutines `suspend` function with `SuspendableBooleanFlagSource`

```kotlin
interface FlagService {
    @BooleanFlag(
      key = "new-awesome-feature",
      defaultValue = false
    )
    @DebugWith(RemoteFlagSource::class)
    @ReleaseWith(False::class)
    suspend fun awesomeFeatureEnabled(): Boolean
}
```

```kotlin
class MyRemoteFlagSource @Inject constructor(
  val api: Api
) : RemoteFlagSource {
  override suspend fun fetch(
    key: String,
    defaultValue: Boolean,
    env: Map<String, Any>
  ): Boolean {
    return api.fetchFlag(key, defaultValue)
  }
}
interface RemoteFlagSource : SuspendableBooleanFlagSource
```

### Custom annotation

```kotlin
annotation class DevelopWith(
  val value: KClass<out FlagSource>
)

class DevelopAnnotationAdapter : AnnotationAdapter<DevelopWith> {
  override fun canHandle(
    annotation: DevelopWith,
    env: Map<String, Any>
  ): Boolean {
    return env[ENV_IS_DEVELOP_KEY] == true
  }

  override fun flagSourceClass(annotation: DevelopWith): KClass<out FlagSource> {
    return annotation.value
  }

  override fun annotationClass(): KClass<DevelopWith> {
    return DevelopWith::class
  }

  companion object {
    const val ENV_IS_DEVELOP_KEY = "ENV_IS_DEVELOP_KEY"
  }
}
```

```kotlin
@DevelopWith(True::class)
fun awesomeFeatureEnabled(): Boolean
```

### Testing

```kotlin
val flagfit = Flagfit(
  flagSources = listOf(),
  baseEnv = mapOf(Flagfit.ENV_IS_DEBUG_KEY to true),
  annotationAdapters = listOf(
    ReleaseAnnotationAdapter(),
    DebugAnnotationAdapter()
  )
)
val flagService: FlagService = flagfit.create()

val awesomeFeatureEnabled = flagService.awesomeFeatureEnabled()

assertThat(awesomeFeatureEnabled).isTrue()
```

### Variation Testing

```kotlin
enum class ABC {
  A, B, C;

  companion object : VariationAdapter<ABC>(ABC::class) {
    override fun variationOf(value: String): ABC {
      return values().firstOrNull { it.name == value } ?: A
    }
  }
}
```

```kotlin
interface Service {
  @VariationFlag(
    key = "variation",
    defaultValue = ABC.DEFAULT_VALUE
  )
  @DefaultWith(LocalFlagSource::class)
  fun variation(): ABC
}
```

```kotlin
val flagfit = Flagfit(
  variationAdapters = listOf(ABC.Companion)
  ...
)
val service = flagfit.create(Service::class)
val abc = service.variation()
```


### Overriding or Adding environment variable for function

```kotlin
@DevelopWith(True::class)
@BooleanEnv(key = ENV_IS_DEVELOP_KEY, value = true)
fun awesomeFeatureEnabled(): Boolean
```

### Debuggable feature


```kotlin
val flagStates: List<Flagfit.FlagState> = flagfit.getFlagStates(FlagService::class)
```

```kotlin
sealed class FlagState(
  open val method: Method,
  // mutable for debug
  open var flagSource: FlagSource?,
  open val isSuspendFunction: Boolean,
  open val env: Map<String, Any>
) {
  data class BooleanFlagState(
    val booleanFlag: BooleanFlag,
    override val method: Method,
    // mutable for debug
    override var flagSource: FlagSource?,
    override val isSuspendFunction: Boolean,
    override val env: Map<String, Any>
  ) : FlagState(method, flagSource, isSuspendFunction, env) {
    fun invokeFlag(service: Any): Boolean {
      return method.invoke(service) as Boolean
    }
  }

  data class VariationFlagState(
    val variationFlag: VariationFlag,
    val variationAdapter: VariationAdapterInterface<*>,
    // mutable for debug
    override val method: Method,
    override var flagSource: FlagSource?,
    override val isSuspendFunction: Boolean,
    override val env: Map<String, Any>
  ) : FlagState(method, flagSource, isSuspendFunction, env) {
    fun invokeFlag(service: Any): Any {
      return method.invoke(service) as Any
    }
  }
}
```

## Flagfit default flag types
This library uses the Flagfit library and provides some intentional flag type annotations according to Feature Toggles articles.  

https://martinfowler.com/articles/feature-toggles.html


### Introduction for default flag types

As we develop, we use Release Toggles, Experiment Toggles, and Opts Toggles for features.  
We develop by switching these flags.  
  
We use the `@WorkInProgress` as Release Toggles when we first start development.  
If the flag using this `@ToggleType.WorkInProgress` is used properly, even if the feature is released, **the false value will be used fixedly**, so the function will not be released by mistake.

```kotlin
@BooleanFlag(
  key = "awesome-feature",
  defaultValue = false
)
@ToggleType.WorkInProgress
fun awesomeFeatureEnabled(): Boolean
```

Next, we do A / B testing and experiment with what we develop.  
So we use `@FlagType.Experiment`. With it, you can use any flag management tool, such as Firebase RemoteConfig, to get the flag and use it. You need to pass a FlagSource that implements `ExperimentFlagSource` when initializing Flagfit.

```kotlin
@BooleanFlag(
  key = "awesome-feature",
  defaultValue = false
)
@FlagType.Experiment
fun awesomeFeatureEnabled(): Boolean
```

Then, in the operation stage, it can be implemented using `@FlagType.Ops` and OpsFlagSource as well.  
If you implement `ExperimentFlagSource` and `OpsFlagSource`, you can use one flag management tool either.

### Setup for default flag types

You can use the default flag types as follows:

```kotlin
class MyLocalFlagSource @Inject constructor(
    val disk: Disk
) : BlockingBooleanFlagSource,
    /* **please implement ExperimentFlagSource ** */
    ExperimentFlagSource {
    override fun get(
        key: String,
        defaultValue: Boolean,
        env: Map<String, Any>
    ): Boolean {
        return disk.readFlag(key, defaultValue)
    }
}

val flagfit = Flagfit(
  // To use @FlagType.Experiment or @FlagType.Ops, you need to set an object that implements ExperimentFlagSource or OpsFlagSource.
  flagSources = listOf(myLocalFlagSource),
  baseEnv = mapOf(
    Flagfit.ENV_IS_DEBUG_KEY to BuildConfig.DEBUG
  ),
  // You need to add annotation adapters to use flag type annotations.
  annotationAdapters = FlagType.annotationAdapters()
)
```
