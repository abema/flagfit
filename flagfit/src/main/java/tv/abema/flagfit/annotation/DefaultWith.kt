package tv.abema.flagfit.annotation

import tv.abema.flagfit.FlagSource
import kotlin.reflect.KClass

annotation class DefaultWith(
  val value: KClass<out FlagSource>
)
