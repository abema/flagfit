# Flagfit
A Flexible Feature Flag Library for Android and Kotlin

Flagfit is a powerful, lightweight library designed to turn your feature flags into easy-to-manage Kotlin interfaces. Whether you're a small startup or a large enterprise, you'll find Flagfit's versatility and simplicity beneficial to your software development cycle.

By integrating Flagfit into your development workflow, you'll be able to:

1. **Efficiently manage feature rollouts**: Toggle new features on or off without deploying new code, giving you the flexibility to test, iterate, and release at your own pace.
2. **Perform A/B testing**: Easily create and manage multiple versions of your app for conducting experiments and making data-driven decisions.
3. **Mitigate risks**: Gradually roll out features to a subset of users to minimize the impact of potential bugs or issues.

In addition, Flagfit provides a set of robust tools for custom flag sources, async flag fetching with Kotlin Coroutines, custom annotations, and more. Use our built-in lint tool to warn about flag expiration times, ensuring that your flags stay up-to-date and relevant.

## Quick Start

### Installation

To incorporate Flagfit into your Android project, add the following dependencies to your `build.gradle` file:


```groovy
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
    implementation 'com.github.abema.flagfit:flagfit:1.1.1'
    // Flagfit flagtype
    implementation 'com.github.abema.flagfit:flagfit-flagtype:1.1.1'
    // Flagfit lint
    lintChecks 'com.github.abema.flagfit:flagfit-lint:1.1.1'
}
```

If you want to warn about the expiration time set in the Flag, please add the **flagfit-lint** library as well.


### Defining Feature Flags

With Flagfit, feature flags are defined using Kotlin interfaces. The `FlagType` annotation specifies different flag types, allowing more control and information about how and when the flag is used:

```kotlin
interface FlagService {
    @BooleanFlag(
      key = "awesome-feature",
      defaultValue = false
    )
    @FlagType.Experiment(
      owner = "{GitHub UserId}",
      // If the flag expires, the lint will warn you.
      expiryDate = "2023-06-13"
    )
    fun awesomeFeatureEnabled(): Boolean
}
```

### Defining FlagSources

A `FlagSource` is an abstraction that reads the actual state of a feature flag from a specific location, such as a server. Flagfit allows you to provide a list of `FlagSource` instances, meaning you can retrieve flags from multiple sources:

```kotlin
class RemoteFlagSource(
    private val remoteFlags: RemoveFlags // Your actual implementation to communicate with server
): BlockingBooleanFlagSource,
    ExperimentFlagSource {

  override fun get(
    key: String,
    defaultValue: Boolean,
    env: Map<String, Any>
  ): Boolean {
    return remoteFlags.get(key, defaultValue)
  }
}
```

### Fetching Feature Flags

```kotlin
val flagfit = Flagfit(
  flagSources = listOf(RemoteFlagSource(flags)),
  annotationAdapters = FlagType.annotationAdapters()
)
```

In this example, `RemoteFlagSource` is a class that communicates with a server to fetch the feature flag. `remoteFlags` is a hypothetical API service that your application uses to communicate with the backend.

Please replace `remoteFlags` and `remoteFlags.get(key, defaultValue)` with your actual implementation to communicate with the server or SDK like Firebase Remote Config.

Flagfit generates an implementation of the `FlagService` interface which can then be invoked:

```kotlin
val flagService: FlagService = flagfit.create()
val awesomeFeatureEnabled = flagService.awesomeFeatureEnabled()
```

### Controlling Features

Feature availability in your application can be controlled based on the flag types. For instance, a feature tagged with the `@WorkInProgress` flag type won't be available when the app is released, preventing unintentional feature release.

Explore further flag types like `@FlagType.WorkInProgress`, `@FlagType.Ops`, and `@FlagType.Permission` in [the FlagType documentation section](https://github.com/abema/flagfit#flagfit-default-flag-types).


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
If the flag using this `@FlagType.WorkInProgress` is used properly, even if the feature is released, **the false value will be used fixedly**, so the function will not be released by mistake.

When using FlagType, please set `owner` and `expiryDate`. [Please see section](./README.md#Lint-check-based-on-expiration-date)

```kotlin
@BooleanFlag(
  key = "awesome-feature",
  defaultValue = false
)
@FlagType.WorkInProgress(
  owner = "{GitHub UserId}",
  expiryDate = "2023-06-13"
)
fun awesomeFeatureEnabled(): Boolean
```

Next, we do A / B testing and experiment with what we develop.  
So we use `@FlagType.Experiment`. With it, you can use any flag management tool, such as Firebase RemoteConfig, to get the flag and use it. You need to pass a FlagSource that implements `ExperimentFlagSource` when initializing Flagfit.

```kotlin
@BooleanFlag(
  key = "awesome-feature",
  defaultValue = false
)
@FlagType.Experiment(
  owner = "{GitHub UserId}",
  expiryDate = "2023-06-13"
)
fun awesomeFeatureEnabled(): Boolean
```

Then, in the operation stage, it can be implemented using `@FlagType.Ops` and OpsFlagSource as well.  
If you implement `ExperimentFlagSource` and `OpsFlagSource`, you can use one flag management tool either.

Since `@FlagType.Ops` and `@FlagType.Permission` may be operated indefinitely, there is no need to set `expiryDate`.

```kotlin
@BooleanFlag(
  key = "awesome-feature",
  defaultValue = false
)
@FlagType.Ops(
  owner = "{GitHub UserId}",
)
fun awesomeFeatureEnabled(): Boolean
```

There may be cases where you do not know the owner or do not want to intentionally generate an error due to not setting a property. In such cases, please set the value as follows

```kotlin
import tv.abema.flagfit.FlagfitDeprecatedParams.EXPIRY_DATE_NOT_DEFINED
import tv.abema.flagfit.FlagfitDeprecatedParams.OWNER_NOT_DEFINED

@BooleanFlag(
  key = "new-awesome-unknown-feature",
  defaultValue = false
)
@FlagType.WorkInProgress(
  owner = OWNER_NOT_DEFINED,
  expiryDate = EXPIRY_DATE_NOT_DEFINED
)
fun awesomeUnknownFeatureEnabled(): Boolean
```

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

## Lint check based on expiration date

Flags that have passed their expiration date or are scheduled to expire within the next 7 days will be displayed as warnings in the IDE.
| Explanation | Image |
|-----|-----|
|When the flag is about to expire|<img width="799" alt="soon" src="https://github.com/abema/flagfit/assets/51113946/7fc78bd3-54ba-44c5-9433-9531563b1388">|
|When the flag has expired|<img width="687" alt="expired" src="https://github.com/abema/flagfit/assets/51113946/804e49b8-1555-413e-9455-f6bf21b6e622">|


### Automatic issue creation via workflow

Flags that have passed their expiration date will be automatically created as issues assigned to the creator through the workflow.
- Please copy the [workflow](./.github/workflows/lintIssues.yml) and [script](./scripts/maintain-flagfit-expiration-issue.main.kts) to the project you are using.
- The workflow allows you to set a cron schedule, so please set it as appropriate.
- When setting feature flags with Flagfit, you will likely use `@BooleanFlag` or `@VariationFlag`, but please make sure that the key value is always unique.
<img width="1250" alt="Sample issues" src="https://github.com/abema/flagfit/assets/51113946/b2a1906d-2493-49c0-a0cd-4fa8fd0d132d">
