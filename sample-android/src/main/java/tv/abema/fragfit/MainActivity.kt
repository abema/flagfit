package tv.abema.fragfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import tv.abema.flagfit.DebugAnnotationAdapter
import tv.abema.flagfit.FlagType
import tv.abema.flagfit.Flagfit
import tv.abema.flagfit.Flagfit.Companion.ENV_IS_DEBUG_KEY
import tv.abema.flagfit.ReleaseAnnotationAdapter
import tv.abema.fragfit.ui.theme.FlagfitSampleTheme

class MainActivity : ComponentActivity() {
  private val sampleFlagSource = SampleFlagSource()

  private val flagfit = Flagfit(
    flagSources = listOf(sampleFlagSource),
    baseEnv = mapOf(
      ENV_IS_DEBUG_KEY to BuildConfig.DEBUG,
    ),
    annotationAdapters = listOf(
      DebugAnnotationAdapter(),
      ReleaseAnnotationAdapter(),
    ) + FlagType.annotationAdapters()
  )

  private val sampleFlagService: SampleFlagService = flagfit.create()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      FlagfitSampleTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          FlagfitSampleComponent(sampleFlagService)
        }
      }
    }
  }
}
