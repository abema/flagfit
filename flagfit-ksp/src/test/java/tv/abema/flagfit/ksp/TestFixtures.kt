package tv.abema.flagfit.ksp

import tv.abema.flagfit.BlockingBooleanFlagSource
import tv.abema.flagfit.OpsFlagSource
import tv.abema.flagfit.SuspendableBooleanFlagSource
import tv.abema.flagfit.VariationAdapter

enum class XYZ {
  X, Y, Z;

  companion object : VariationAdapter<XYZ>(XYZ::class) {
    override fun variationOf(value: String): XYZ {
      return values().firstOrNull { it.name == value } ?: X
    }
  }
}

object FakeSuspendableBooleanFlagSource : SuspendableBooleanFlagSource {
  override suspend fun fetch(
    key: String,
    defaultValue: Boolean,
    env: Map<String, Any>,
  ): Boolean {
    return true
  }
}

object FakeOpsFlagSource : OpsFlagSource, BlockingBooleanFlagSource {
  override fun get(
    key: String,
    defaultValue: Boolean,
    env: Map<String, Any>,
  ): Boolean {
    return true
  }
}
