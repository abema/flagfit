package tv.abema.flagfit.annotation

import tv.abema.flagfit.FlagSource
import kotlin.reflect.KClass

annotation class DebugWith(
  val value: KClass<out FlagSource>
)
