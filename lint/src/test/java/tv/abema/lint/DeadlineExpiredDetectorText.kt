package tv.abema.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import tv.abema.lint.DeadlineExpiredDetector.Companion.ISSUE_DEADLINE_EXPIRED

@RunWith(JUnit4::class)
class DeadlineExpiredDetectorText : LintDetectorTest() {

  private lateinit var stabBooleanFlag: TestFile
  private lateinit var stabFlagType: TestFile

  @Before
  fun before() {
    stabBooleanFlag = kotlin(
      """
      package tv.abema.flagfit.annotation
      
      annotation class BooleanFlag(
         val key: String,
         val defaultValue: Boolean
       )
      """.trimIndent()
    )
    stabFlagType = kotlin(
      """
      package tv.abema.flagfit
      
      class FlagType {
        annotation class Ops(
          val author: String,
          val description: String,
          val expiryDate: String,
        )
      }
      """.trimIndent()
    )
  }

  @Test
  fun testFlagTypeOpsExpiryWarning() {
    lint()
      .files(
        stabBooleanFlag,
        stabFlagType,
        kotlin(
          """
          package foo
          import tv.abema.flagfit.FlagType
          import tv.abema.flagfit.annotation.BooleanFlag
          
          interface Example {
              @BooleanFlag(
                key = "new-awesome-feature",
                defaultValue = false
              )
              @FlagType.Ops(
                author = "Hoge Fuga",
                description = "hogehoge",
                expiryDate = "2022-12-30"
              )
              fun awesomeWipFeatureEnabled(): Boolean
          }
          """.trimIndent()
        )
      )
      .issues(*issues.toTypedArray())
      .allowMissingSdk()
      .run()
      .expect(
        """
        src/foo/Example.kt:10: Warning: Your @FlagType.Ops has expired!
        Ops has expired. Please consider deleting @FlagType.Ops [DeadlineExpired]
            @FlagType.Ops(
            ^
        0 errors, 1 warnings
        """.trimIndent()
      )
  }

  @Test
  fun testFlagTypeOpsNoExpiryWarning() {
    lint()
      .files(
        stabBooleanFlag,
        stabFlagType,
        kotlin(
          """
          package foo
          import tv.abema.flagfit.FlagType
          import tv.abema.flagfit.annotation.BooleanFlag
          
          interface Example {
              @BooleanFlag(
                key = "new-awesome-feature",
                defaultValue = false
              )
              @FlagType.Ops(
                author = "Hoge Fuga",
                description = "hogehoge",
                expiryDate = "3022-12-30"
              )
              fun awesomeWipFeatureEnabled(): Boolean
          }
          """.trimIndent()
        )
      )
      .issues(*issues.toTypedArray())
      .allowMissingSdk()
      .run()
      .expectClean()
  }

  override fun getDetector(): Detector = DeadlineExpiredDetector()

  override fun getIssues(): MutableList<Issue> {
    return mutableListOf(
      ISSUE_DEADLINE_EXPIRED
    )
  }
}
