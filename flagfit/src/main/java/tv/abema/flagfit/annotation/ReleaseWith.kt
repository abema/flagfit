package tv.abema.flagfit.annotation

import tv.abema.flagfit.FlagSource
import kotlin.reflect.KClass

annotation class ReleaseWith(
  val value: KClass<out FlagSource>
)
