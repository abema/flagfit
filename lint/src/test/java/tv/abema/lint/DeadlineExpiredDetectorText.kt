package tv.abema.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import tv.abema.lint.DeadlineExpiredDetector.Companion.CURRENT_TIME
import tv.abema.lint.DeadlineExpiredDetector.Companion.ISSUE_DEADLINE_EXPIRED
import tv.abema.lint.DeadlineExpiredDetector.Companion.ISSUE_DEADLINE_SOON
import tv.abema.lint.DeadlineExpiredDetector.Companion.TIME_ZONE

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
        annotation class Experiment(
          val author: String,
          val description: String,
          val expiryDate: String,
        )
      }
      """.trimIndent()
    )
  }

  @Test
  fun testFlagTypeExperimentExpiryWarning() {
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
              @FlagType.Experiment(
                author = "Hoge Fuga",
                description = "hogehoge",
                expiryDate = "2023-06-01"
              )
              fun awesomeWipFeatureEnabled(): Boolean
          }
          """.trimIndent()
        )
      )
      .issues(*issues.toTypedArray())
      .allowMissingSdk()
      .configureOption(TIME_ZONE, "Asia/Tokyo")
      .configureOption(CURRENT_TIME, "2023-06-02")
      .run()
      .expect(
        """
        src/foo/Example.kt:10: Warning: The @FlagType.Experiment created by Hoge Fuga has expired!
        Please consider deleting @FlagType.Experiment as the expiration date has passed on 2023-06-01. [FlagfitDeadlineExpired]
            @FlagType.Experiment(
            ^
        0 errors, 1 warnings
        """.trimIndent()
      )
  }

  @Test
  fun testFlagTypeExperimentSoonExpiryWarning() {
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
              @FlagType.Experiment(
                author = "Hoge Fuga",
                description = "hogehoge",
                expiryDate = "2023-06-01",
              )
              fun awesomeWipFeatureEnabled(): Boolean
          }
          """.trimIndent()
        )
      )
      .issues(*issues.toTypedArray())
      .allowMissingSdk()
      .configureOption(TIME_ZONE, "Asia/Tokyo")
      .configureOption(CURRENT_TIME, "2023-05-26")
      .run()
      .expect(
        """
        src/foo/Example.kt:10: Warning: The @FlagType.Experiment Hoge Fuga will expire soon!
        Please consider deleting @FlagType.Experiment as the expiry date of 2023-06-01 is scheduled to pass within a week. [FlagfitDeadlineSoon]
            @FlagType.Experiment(
            ^
        0 errors, 1 warnings
        """.trimIndent()
      )
  }

  @Test
  fun testFlagTypeExperimentNoExpiryWarning() {
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
              @FlagType.Experiment(
                author = "Hoge Fuga",
                description = "hogehoge",
                expiryDate = "2023-06-01",
              )
              fun awesomeWipFeatureEnabled(): Boolean
          }
          """.trimIndent()
        )
      )
      .issues(*issues.toTypedArray())
      .allowMissingSdk()
      .configureOption(TIME_ZONE, "Asia/Tokyo")
      .configureOption(CURRENT_TIME, "2023-05-25")
      .run()
      .expectClean()
  }

  @Test
  fun testNoExpiredDateFlagTypeNoExpiryWarning() {
    lint()
      .files(
        stabBooleanFlag,
        kotlin(
          """
          package tv.abema.flagfit

          class FlagType {
            annotation class Ops(
              val author: String,
              val description: String,
              val expiryDate: String = "",
            )
          }
          """.trimIndent()
        ),
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
                description = "hogehoge"
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
      ISSUE_DEADLINE_EXPIRED,
      ISSUE_DEADLINE_SOON
    )
  }
}
