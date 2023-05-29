package tv.abema.fragfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tv.abema.flagfit.DebugAnnotationAdapter
import tv.abema.flagfit.Flagfit
import tv.abema.flagfit.Flagfit.Companion.ENV_IS_DEBUG_KEY
import tv.abema.flagfit.ReleaseAnnotationAdapter
import tv.abema.fragfit.ui.theme.FlagfitSampleTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      FlagfitSampleTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          Column(
            verticalArrangement = Arrangement.Center
          ) {
            FragfitSampleComponent()
          }
        }
      }
    }
  }
}

@Composable
fun FragfitSampleComponent() {
  val flagfit = Flagfit(
    baseEnv = mapOf(
      ENV_IS_DEBUG_KEY to BuildConfig.DEBUG,
    ),
    annotationAdapters = listOf(
      DebugAnnotationAdapter(),
      ReleaseAnnotationAdapter()
    )
  )
  val sampleFlagService: SampleFlagService = flagfit.create()

  val awesomeExperimentFeatureEnabled = sampleFlagService.awesomeExperimentFeatureEnabled()
  val awesomeOpsFeatureEnabled = sampleFlagService.awesomeOpsFeatureEnabled()

  val experimentText = if (awesomeExperimentFeatureEnabled) "New Function" else "Previous function"
  val opsText = if (awesomeOpsFeatureEnabled) "New Function" else "Previous function"

  Text(text = "Experiment: $experimentText")
  Text(text = "Ops: $opsText")
}
