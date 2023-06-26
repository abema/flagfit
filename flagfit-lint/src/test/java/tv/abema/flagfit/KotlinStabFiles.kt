package tv.abema.flagfit

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile

object KotlinStabFiles {
  val stabBooleanFlag: TestFile = LintDetectorTest.kotlin(
    """
      package tv.abema.flagfit.annotation
      
      annotation class BooleanFlag(
         val key: String,
         val defaultValue: Boolean
       )
      """.trimIndent()
  )
  val stabFlagType: TestFile = LintDetectorTest.kotlin(
    """
      package tv.abema.flagfit
      
      class FlagType {
        annotation class WorkInProgress(
          val owner: String,
          val expiryDate: String,
        )
        annotation class Experiment(
          val owner: String,
          val expiryDate: String,
        )
        annotation class Ops(
          val owner: String,
          val expiryDate: String,
        )
        annotation class Permission(
            val owner: String,
            val expiryDate: String,
          )
        companion object {
            const val EXPIRY_DATE_INFINITE = "EXPIRY_DATE_INFINITE"
            @Deprecated("Flag with no assigned owner")
            const val OWNER_NOT_DEFINED = "OWNER_NOT_DEFINED"
            @Deprecated("Flag without an expiry date")
            const val EXPIRY_DATE_NOT_DEFINED = "EXPIRY_DATE_NOT_DEFINED"
          }
      }
      """.trimIndent()
  )
}
