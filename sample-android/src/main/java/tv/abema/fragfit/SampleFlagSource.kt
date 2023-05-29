package tv.abema.fragfit

import tv.abema.flagfit.BlockingBooleanFlagSource
import tv.abema.flagfit.ExperimentFlagSource
import tv.abema.flagfit.OpsFlagSource
import tv.abema.flagfit.PermissionFlagSource

class SampleFlagSource : BlockingBooleanFlagSource,
  ExperimentFlagSource,
  OpsFlagSource,
  PermissionFlagSource {
  override fun get(
    key: String,
    defaultValue: Boolean,
    env: Map<String, Any>,
  ): Boolean {
    // Write a process to read in the value of the flag
    return true
  }
}
