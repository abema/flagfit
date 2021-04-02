package tv.abema.flagfit

import kotlin.reflect.KClass

interface AnnotationAdapter<T : Annotation> {
  fun annotationClass(): KClass<T>
  fun canHandle(annotation: T, env: Map<String, Any>): Boolean
  fun flagType(annotation: T): KClass<out FlagSource>
}
