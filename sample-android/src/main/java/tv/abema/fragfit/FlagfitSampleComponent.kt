package tv.abema.fragfit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun FlagfitSampleComponent(sampleFlagService: SampleFlagService) {

  val awesomeWipFeatureEnabled = sampleFlagService.awesomeWipFeatureEnabled()
  val awesomeExperimentFeatureEnabled = sampleFlagService.awesomeExperimentFeatureEnabled()
  val awesomeOpsFeatureEnabled = sampleFlagService.awesomeOpsFeatureEnabled()
  val awesomePermissionFutureEnabled = sampleFlagService.awesomePermissionFeatureEnabled()
  val awesomeDebugFutureEnabled = sampleFlagService.awesomeDebugFeatureEnabled()

  val wipText = if (awesomeWipFeatureEnabled) "New Function" else "Previous function"
  val experimentText = if (awesomeExperimentFeatureEnabled) "New Function" else "Previous function"
  val opsText = if (awesomeOpsFeatureEnabled) "New Function" else "Previous function"
  val permissionText = if (awesomePermissionFutureEnabled) "New Function" else "Previous function"
  val debugText = if (awesomeDebugFutureEnabled) "New Function" else "Previous function"

  Column(
    verticalArrangement = Arrangement.Center
  ) {
    Text(text = "Work in progress: $wipText")
    Text(text = "Experiment: $experimentText")
    Text(text = "Ops: $opsText")
    Text(text = "Permission: $permissionText")
    Text(text = "Debug: $debugText")
  }
}
