package tv.abema.flagfit

import tv.abema.flagfit.annotation.ReleaseWith
import java.time.LocalDate
import kotlin.reflect.KClass

class ReleaseAnnotationAdapter : AnnotationAdapter<ReleaseWith> {
  override fun canHandle(
    annotation: ReleaseWith,
    env: Map<String, Any>,
  ): Boolean {
    return env[Flagfit.ENV_IS_DEBUG_KEY] == false
  }

  override fun flagSourceClass(annotation: ReleaseWith): KClass<out FlagSource> {
    return annotation.value
  }

  override fun annotationClass(): KClass<ReleaseWith> {
    return ReleaseWith::class
  }

  override fun flagMetaData(annotation: ReleaseWith): FlagMetadata {
    return FlagMetadata(
      author = "",
      description = "",
      expiryDate = LocalDate.now()
    )
  }
}
