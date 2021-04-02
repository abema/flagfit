package tv.abema.flagfit

import tv.abema.flagfit.annotation.DefaultWith
import kotlin.reflect.KClass

class DefaultAnnotationAdapter : AnnotationAdapter<DefaultWith> {
  override fun canHandle(
    annotation: DefaultWith,
    env: Map<String, Any>
  ): Boolean {
    return true
  }

  override fun flagType(annotation: DefaultWith): KClass<out FlagSource> {
    return annotation.value
  }

  override fun annotationClass(): Class<DefaultWith> {
    return DefaultWith::class.java
  }
}
