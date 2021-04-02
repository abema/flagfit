package tv.abema.flagfit

import kotlin.reflect.KClass

interface VariationAdapterInterface<T : Any> {
  fun variationOf(value: String): T
  fun variationType(): KClass<T>
}

abstract class VariationAdapter<T : Any>(
  private val clazz: KClass<T>
) : VariationAdapterInterface<T> {
  override fun variationType(): KClass<T> = clazz
}
