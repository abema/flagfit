package tv.abema.fragfit

import tv.abema.flagfit.FlagType
import tv.abema.flagfit.FlagfitDeprecatedParams.EXPIRY_DATE_NOT_DEFINED
import tv.abema.flagfit.FlagfitDeprecatedParams.OWNER_NOT_DEFINED
import tv.abema.flagfit.JustFlagSource
import tv.abema.flagfit.annotation.BooleanFlag
import tv.abema.flagfit.annotation.DebugWith
import tv.abema.flagfit.annotation.ReleaseWith

interface SampleFlagService {

  @BooleanFlag(
    key = "new-awesome-wip-feature",
    defaultValue = false
  )
  @FlagType.WorkInProgress(
    owner = "Hoge Fuga",
    expiryDate = "2022-12-30"
  )
  fun awesomeWipFeatureEnabled(): Boolean

  @BooleanFlag(
    key = "new-awesome-experiment-feature",
    defaultValue = false
  )
  @FlagType.Experiment(
    owner = "Hoge Fuga",
    expiryDate = "2322-12-30"
  )
  fun awesomeExperimentFeatureEnabled(): Boolean

  @BooleanFlag(
    key = "new-awesome-ops-feature",
    defaultValue = false
  )
  @FlagType.Ops(
    owner = "Hoge Fuga",
  )
  fun awesomeOpsFeatureEnabled(): Boolean

  @BooleanFlag(
    key = "new-awesome-permission-feature",
    defaultValue = false
  )
  @FlagType.Permission(
    owner = "Hoge Fuga",
    expiryDate = "2022-12-30"
  )
  fun awesomePermissionFeatureEnabled(): Boolean

  @BooleanFlag(
    key = "new-awesome-unknown-feature",
    defaultValue = false
  )
  @FlagType.WorkInProgress(
    owner = OWNER_NOT_DEFINED,
    expiryDate = EXPIRY_DATE_NOT_DEFINED
  )
  fun awesomeUnknownFeatureEnabled(): Boolean

  @BooleanFlag(
    key = "new-awesome-debug-feature",
    defaultValue = false
  )
  @DebugWith(JustFlagSource.True::class)
  @ReleaseWith(JustFlagSource.False::class)
  fun awesomeDebugFeatureEnabled(): Boolean
}
