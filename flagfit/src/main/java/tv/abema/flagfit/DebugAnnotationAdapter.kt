package tv.abema.flagfit

import tv.abema.flagfit.annotation.DebugWith
import java.time.LocalDate
import kotlin.reflect.KClass

class DebugAnnotationAdapter : AnnotationAdapter<DebugWith> {
  override fun canHandle(
    annotation: DebugWith,
    env: Map<String, Any>,
  ): Boolean {
    return env[Flagfit.ENV_IS_DEBUG_KEY] == true
  }

  override fun flagSourceClass(annotation: DebugWith): KClass<out FlagSource> {
    return annotation.value
  }

  override fun annotationClass(): KClass<DebugWith> {
    return DebugWith::class
  }

  override fun flagMetaData(annotation: DebugWith): FlagMetadata {
    return FlagMetadata(
      author = "",
      description = "",
      expiryDate = LocalDate.now()
    )
  }
}
