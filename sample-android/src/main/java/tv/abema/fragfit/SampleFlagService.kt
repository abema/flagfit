package tv.abema.fragfit

import tv.abema.flagfit.FlagType
import tv.abema.flagfit.JustFlagSource
import tv.abema.flagfit.annotation.BooleanFlag
import tv.abema.flagfit.annotation.DebugWith
import tv.abema.flagfit.annotation.ReleaseWith

interface SampleFlagService {

  @BooleanFlag(
    key = "new-awesome-wip-feature",
    defaultValue = false
  )
  @FlagType.WorkInProgress
  fun awesomeWipFeatureEnabled(): Boolean

  @BooleanFlag(
    key = "new-awesome-experiment-feature",
    defaultValue = false
  )
  @FlagType.Experiment
  fun awesomeExperimentFeatureEnabled(): Boolean

  @BooleanFlag(
    key = "new-awesome-ops-feature",
    defaultValue = false
  )
  @FlagType.Ops
  fun awesomeOpsFeatureEnabled(): Boolean

  @BooleanFlag(
    key = "new-awesome-permission-feature",
    defaultValue = false
  )
  @FlagType.Permission
  fun awesomePermissionFeatureEnabled(): Boolean

  @BooleanFlag(
    key = "new-awesome-debug-feature",
    defaultValue = false
  )
  @DebugWith(JustFlagSource.True::class)
  @ReleaseWith(JustFlagSource.False::class)
  fun awesomeDebugFeatureEnabled(): Boolean
}
