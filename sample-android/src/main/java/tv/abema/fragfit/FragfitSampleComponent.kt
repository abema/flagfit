package tv.abema.fragfit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun FragfitSampleComponent(sampleFlagService: SampleFlagService) {

  val awesomeExperimentFeatureEnabled = sampleFlagService.awesomeExperimentFeatureEnabled()
  val awesomeOpsFeatureEnabled = sampleFlagService.awesomeOpsFeatureEnabled()

  val experimentText = if (awesomeExperimentFeatureEnabled) "New Function" else "Previous function"
  val opsText = if (awesomeOpsFeatureEnabled) "New Function" else "Previous function"

  Column(
    verticalArrangement = Arrangement.Center
  ) {
    Text(text = "Experiment: $experimentText")
    Text(text = "Ops: $opsText")
  }
}
