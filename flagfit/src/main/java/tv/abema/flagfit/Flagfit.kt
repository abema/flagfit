package tv.abema.flagfit

import tv.abema.flagfit.annotation.BooleanEnv
import tv.abema.flagfit.annotation.BooleanFlag
import tv.abema.flagfit.annotation.VariationFlag
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.startCoroutine
import kotlin.reflect.KClass

class Flagfit(
  flagSources: List<FlagSource> = listOf(),
  val baseEnv: Map<String, Any> = mapOf(),
  val variationAdapters: List<VariationAdapterInterface<*>> = listOf(),
  annotationAdapters: List<AnnotationAdapter<out Annotation>> = mutableListOf()
) {
  private val flagSources = flagSources + JustFlagSource.True + JustFlagSource.False
  private val classToFlagSourceCache = mutableMapOf<Class<*>, FlagSource>()
  private val annotationAdapters: List<AnnotationAdapter<out Annotation>> =
    annotationAdapters + DefaultAnnotationAdapter()
  private val flagStateCache = mutableMapOf<Method, FlagState>()

  inline fun <reified T : Any> create(): T {
    val serviceClass = T::class
    return create(serviceClass)
  }

  fun <T : Any> create(serviceKClass: KClass<T>): T {
    val serviceClass = serviceKClass.java
    validateAndCacheServiceInterface(serviceClass)
    val proxy = serviceClass.cast(
      Proxy.newProxyInstance(
        serviceClass.classLoader,
        arrayOf<Class<*>>(serviceClass),
        object : InvocationHandler {
          override fun invoke(proxy: Any, method: Method, args: Array<Any>?): Any? {
            when (val flagState = resolveFlagState(method)) {
              is FlagState.BooleanFlagState -> {
                val (
                  flagAnnotation,
                  _,
                  flagSource,
                  isSuspendFunction,
                  env
                ) = flagState
                return when (flagSource) {
                  null -> flagAnnotation.defaultValue
                  else ->
                    flagSource
                      .callFlag(
                        isSuspendFunction = isSuspendFunction,
                        flagAnnotation = flagAnnotation,
                        args = args,
                        env = env
                      )
                }
              }
              is FlagState.VariationFlagState -> {
                val (
                  flagAnnotation,
                  variationAdapter,
                  _,
                  flagSource,
                  isSuspendFunction,
                  env
                ) = flagState

                return when (flagSource) {
                  null -> variationAdapter.variationOf(
                    flagAnnotation.defaultValue
                  )
                  else -> {
                    flagSource
                      .callFlag(
                        isSuspendFunction = isSuspendFunction,
                        flagAnnotation = flagAnnotation,
                        variationAdapter = variationAdapter,
                        args = args,
                        env = env
                      )
                  }
                }
              }
            }
          }
        }
      )
    )
    return requireNotNull(proxy)
  }

  private fun getFlagSourceByClass(flagType: Class<out FlagSource>): FlagSource {
    return classToFlagSourceCache.getOrPut(flagType) {
      flagSources.first {
        flagType.isAssignableFrom(it.javaClass)
      }
    }
  }

  private fun validateAndCacheServiceInterface(service: Class<*>) {
    require(service.isInterface) { "Flag declarations must be interfaces." }

    for (method in service.declaredMethods) {
      if (!Modifier.isStatic(method.modifiers)) {
        resolveFlagState(method)
      }
    }
  }

  fun getFlagStates(service: KClass<*>): List<FlagState> {
    val serviceClass = service.java
    require(serviceClass.isInterface) { "Flag declarations must be interfaces." }

    return serviceClass.declaredMethods
      .mapNotNull { method ->
        if (!Modifier.isStatic(method.modifiers)) {
          resolveFlagState(method)
        } else {
          null
        }
      }
  }

  private fun checkLastParameter(parameters: Array<Class<*>>) {
    val isSuspendFunction = parameters.lastOrNull() == Continuation::class.java
    if (isSuspendFunction) {
      check(
        flagSources.all { it is SuspendableBooleanFlagSource || it is SuspendableStringFlagSource }
      ) {
        "flagSources should be SuspendableXXFlagSource"
      }
    } else {
      check(flagSources.all { it is BlockingBooleanFlagSource || it is BlockingStringFlagSource }) {
        "flagSources should be BlockingXXFlagSource"
      }
    }
  }

  private fun FlagSource.callFlag(
    flagAnnotation: BooleanFlag,
    isSuspendFunction: Boolean,
    args: Array<Any>?,
    env: Map<String, Any>
  ): Any {
    val flagSource = this
    return if (isSuspendFunction) {
      if (flagSource is SuspendableBooleanFlagSource) {
        @Suppress("UNCHECKED_CAST")
        val continuation = checkNotNull(args)[args.size - 1] as Continuation<Boolean>
        suspend {
          flagSource.fetch(flagAnnotation.key, flagAnnotation.defaultValue, env)
        }.startCoroutine(continuation)
        kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
      } else {
        throw IllegalArgumentException("FlagSource should be SuspendableFlagSource")
      }
    } else {
      if (flagSource is BlockingBooleanFlagSource) {
        flagSource.get(flagAnnotation.key, flagAnnotation.defaultValue, env)
      } else {
        throw IllegalArgumentException("FlagSource should be BlockingFlagSource")
      }
    }
  }

  private fun FlagSource.callFlag(
    flagAnnotation: VariationFlag,
    isSuspendFunction: Boolean,
    variationAdapter: VariationAdapterInterface<*>,
    args: Array<Any>?,
    env: Map<String, Any>
  ): Any {
    val flagSource = this
    return if (isSuspendFunction) {
      if (flagSource is SuspendableStringFlagSource) {
        @Suppress("UNCHECKED_CAST")
        val continuation = checkNotNull(args)[args.size - 1] as Continuation<Any?>
        suspend {
          variationAdapter.variationOf(
            flagSource.fetch(flagAnnotation.key, flagAnnotation.defaultValue, env)
          )
        }.startCoroutine(continuation)
        kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
      } else {
        throw IllegalArgumentException("FlagSource should be SuspendableFlagSource")
      }
    } else {
      if (flagSource is BlockingStringFlagSource) {
        variationAdapter.variationOf(
          flagSource.get(flagAnnotation.key, flagAnnotation.defaultValue, env)
        )
      } else {
        throw IllegalArgumentException("FlagSource should be BlockingFlagSource")
      }
    }
  }

  private fun resolveFlagState(
    method: Method
  ): FlagState {
    return flagStateCache.getOrPut(method) {
      checkLastParameter(method.parameterTypes)

      val annotations = method.annotations
      val booleanFlagAnnotation = annotations
        .filterIsInstance<BooleanFlag>()
        .firstOrNull()
      val variationFlagAnnotation = annotations
        .filterIsInstance<VariationFlag>()
        .firstOrNull()
      val annotationBooleanEnvList: List<BooleanEnv> = annotations
        .filterIsInstance<BooleanEnv>()
      val env = baseEnv + annotationBooleanEnvList
        .associate { it.key to it.value }

      var annotatedFlagSourceType: Class<out FlagSource>? = null
      for (annotationAdapter in annotationAdapters) {
        val annotationClass = annotationAdapter.annotationClass().java

        val annotation = annotations
          .filterIsInstance(annotationClass)
          .firstOrNull() ?: continue

        @Suppress("UNCHECKED_CAST")
        val adapter = annotationAdapter as AnnotationAdapter<Annotation>
        if (adapter.canHandle(annotation, env)) {
          annotatedFlagSourceType = adapter.flagSourceType(annotation).java
          break
        }
      }

      val flagSource = annotatedFlagSourceType?.let {
        try {
          getFlagSourceByClass(annotatedFlagSourceType)
        } catch (e: NoSuchElementException) {
          throw IllegalArgumentException(
            "Flag source($annotatedFlagSourceType) not found for method: ${method.name}"
          )
        }
      }
      val isSuspendFunction = method.parameterTypes.isSuspendFunctionArgs()
      when {
        booleanFlagAnnotation != null -> FlagState.BooleanFlagState(
          booleanFlag = booleanFlagAnnotation,
          method = method,
          isSuspendFunction = isSuspendFunction,
          flagSource = flagSource,
          env = env
        )
        variationFlagAnnotation != null -> {
          val returnType = if (!isSuspendFunction) {
            method.returnType
          } else {
            // When suspend function, We can not find return type, so we introduced @SuspendReturnType
            val typeAnnotation: SuspendReturnType = method.annotations
              .filterIsInstance<SuspendReturnType>()
              .firstOrNull() ?: error("Currently suspend function needs @SuspendReturnType")
            typeAnnotation.value.java
          }
          val variationAdapter = variationAdapters.firstOrNull {
            it.variationType().java == returnType
          } ?: error("VariationAdapterInterface is not found for ${method.returnType}")
          FlagState.VariationFlagState(
            variationFlag = variationFlagAnnotation,
            variationAdapter = variationAdapter,
            method = method,
            flagSource = flagSource,
            isSuspendFunction = isSuspendFunction,
            env = env
          )
        }
        else -> error("@BooleanFlag or @VariationFlag annotation is required")
      }
    }
  }

  private fun Array<Class<*>>?.isSuspendFunctionArgs(): Boolean {
    return this?.lastOrNull() == Continuation::class.java
  }

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

  companion object {
    const val ENV_IS_DEBUG_KEY = "ENV_IS_DEBUG_KEY"
  }
}
