package tv.abema.flagfit

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import tv.abema.flagfit.DeadlineExpiredDetector.Companion.CURRENT_TIME
import tv.abema.flagfit.DeadlineExpiredDetector.Companion.ISSUE_DEADLINE_EXPIRED
import tv.abema.flagfit.DeadlineExpiredDetector.Companion.ISSUE_DEADLINE_SOON
import tv.abema.flagfit.DeadlineExpiredDetector.Companion.ISSUE_ILLEGAL_NO_EXPIRE_PARAM
import tv.abema.flagfit.DeadlineExpiredDetector.Companion.TIME_ZONE

@RunWith(JUnit4::class)
class DeadlineExpiredDetectorText : LintDetectorTest() {

  private lateinit var stabBooleanFlag: TestFile
  private lateinit var stabFlagType: TestFile
  private lateinit var stabDeprecatedInfo: TestFile
  private lateinit var stabExpansionInfo: TestFile

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
      }
      """.trimIndent()
    )
    stabDeprecatedInfo = kotlin(
      """
      package tv.abema.flagfit
      
      object FlagfitDeprecatedParams {
        @Deprecated("Flag with no assigned owner")
        const val OWNER_NOT_DEFINED = "OWNER_NOT_DEFINED"
      
        @Deprecated("Flag without an expiry date")
        const val EXPIRY_DATE_NOT_DEFINED = "EXPIRY_DATE_NOT_DEFINED"
      }
      """.trimIndent()
    )
    stabExpansionInfo = kotlin(
      """
      package tv.abema.flagfit
      
      object FlagfitExpansionParams {
        const val NO_EXPIRY_DATE = "NO_EXPIRY_DATE"
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
                owner = "Hoge Fuga",
                expiryDate = "2023-06-01"
              )
              fun awesomeExperimentFeatureEnabled(): Boolean
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
        src/foo/Example.kt:10: Warning: The @FlagType.Experiment created by owner: Hoge Fuga has expired!
        Please consider deleting @FlagType.Experiment as the expiration date has passed on 2023-06-01.
        The flag of key: "new-awesome-feature" is used in the awesomeExperimentFeatureEnabled function.
         [FlagfitDeadlineExpired]
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
                owner = "Hoge Fuga",
                expiryDate = "2023-06-01",
              )
              fun awesomeExperimentFeatureEnabled(): Boolean
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
        src/foo/Example.kt:10: Warning: The @FlagType.Experiment owner: Hoge Fuga will expire soon!
        Please consider deleting @FlagType.Experiment as the expiry date of 2023-06-01 is scheduled to pass within a week.
        The flag of key: "new-awesome-feature" is used in the awesomeExperimentFeatureEnabled function.
         [FlagfitDeadlineSoon]
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
                owner = "Hoge Fuga",
                expiryDate = "2023-06-01",
              )
              fun awesomeExperimentFeatureEnabled(): Boolean
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
        stabExpansionInfo,
        stabFlagType,
        kotlin(
          """
          package foo
          import tv.abema.flagfit.FlagType
          import tv.abema.flagfit.annotation.BooleanFlag
          import tv.abema.flagfit.FlagfitExpansionParams.NO_EXPIRY_DATE
          
          interface Example {
              @BooleanFlag(
                key = "new-ops-awesome-feature",
                defaultValue = false
              )
              @FlagType.Ops(
                owner = "Hoge Fuga",
                expiryDate = NO_EXPIRY_DATE
              )
              fun awesomeOpsFeatureEnabled(): Boolean
              @BooleanFlag(
                key = "new-permission-awesome-feature",
                defaultValue = false
              )
              @FlagType.Permission(
                owner = "Hoge Fuga",
                expiryDate = NO_EXPIRY_DATE
              )
              fun awesomePermissionFeatureEnabled(): Boolean
          }
          """.trimIndent()
        )
      )
      .issues(*issues.toTypedArray())
      .allowMissingSdk()
      .run()
      .expectClean()
  }

  @Test
  fun testVariationFlagFlagTypeWarning() {
    lint()
      .files(
        stabFlagType,
        kotlin(
          """
          package tv.abema.flagfit.annotation
          
          annotation class VariationFlag(
             val key: String,
             val defaultValue: String
           )
          """.trimIndent()
        ),
        kotlin(
          """
          package foo
          import tv.abema.flagfit.FlagType
          import tv.abema.flagfit.annotation.VariationFlag
          
          interface Example {
              @VariationFlag(
                key = "new-awesome-feature",
                defaultValue = "hoge"
              )
              @FlagType.Experiment(
                owner = "Hoge Fuga",
                expiryDate = "2023-06-01",
              )
              fun awesomeVariationFeatureEnabled(): Boolean
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
        src/foo/Example.kt:10: Warning: The @FlagType.Experiment created by owner: Hoge Fuga has expired!
        Please consider deleting @FlagType.Experiment as the expiration date has passed on 2023-06-01.
        The flag of key: "new-awesome-feature" is used in the awesomeVariationFeatureEnabled function.
         [FlagfitDeadlineExpired]
            @FlagType.Experiment(
            ^
        0 errors, 1 warnings
        """.trimIndent()
      )
  }

  @Test
  fun testDeprecatedFlagTypeNoWarning() {
    lint()
      .files(
        stabBooleanFlag,
        stabFlagType,
        stabDeprecatedInfo,
        kotlin(
          """
          package foo
          import tv.abema.flagfit.FlagType
          import tv.abema.flagfit.annotation.BooleanFlag
          import tv.abema.flagfit.FlagfitDeprecatedParams.OWNER_NOT_DEFINED
          import tv.abema.flagfit.FlagfitDeprecatedParams.EXPIRY_DATE_NOT_DEFINED
          
          interface Example {
              @BooleanFlag(
                key = "new-awesome-feature",
                defaultValue = false
              )
              @FlagType.Experiment(
                owner = OWNER_NOT_DEFINED,
                expiryDate = EXPIRY_DATE_NOT_DEFINED,
              )
              fun awesomeOpsFeatureEnabled(): Boolean
          }
          """.trimIndent()
        )
      )
      .issues(*issues.toTypedArray())
      .allowMissingSdk()
      .run()
      .expectClean()
  }

  @Test
  fun testIllegalNoExpireParamWarning() {
    lint()
      .files(
        stabBooleanFlag,
        stabFlagType,
        stabExpansionInfo,
        stabDeprecatedInfo,
        kotlin(
          """
          package foo
          import tv.abema.flagfit.FlagType
          import tv.abema.flagfit.annotation.BooleanFlag
          import tv.abema.flagfit.FlagfitDeprecatedParams.OWNER_NOT_DEFINED
          import tv.abema.flagfit.FlagfitExpansionParams.NO_EXPIRY_DATE
          
          interface Example {
              @BooleanFlag(
                key = "new-ops-awesome-feature",
                defaultValue = false
              )
              @FlagType.WorkInProgress(
                owner = "Hoge Fuga",
                expiryDate = NO_EXPIRY_DATE
              )
              fun awesomeOpsFeatureEnabled(): Boolean
              @BooleanFlag(
                key = "new-permission-awesome-feature",
                defaultValue = false
              )
              @FlagType.Experiment(
                owner = OWNER_NOT_DEFINED,
                expiryDate = NO_EXPIRY_DATE
              )
              fun awesomePermissionFeatureEnabled(): Boolean
          }
          """.trimIndent()
        )
      )
      .issues(*issues.toTypedArray())
      .allowMissingSdk()
      .run()
      .expect(
        """
        src/foo/Example.kt:12: Error: NO_EXPIRE_DATE cannot be set for the expireDate of @FlagType.WorkInProgress and @FlagType.Experiment.
        Please set the expiration date in the following format: "yyyy-mm-dd" [FlagfitIllegalNoExpireParam]
            @FlagType.WorkInProgress(
            ^
        src/foo/Example.kt:21: Error: NO_EXPIRE_DATE cannot be set for the expireDate of @FlagType.WorkInProgress and @FlagType.Experiment.
        Please set the expiration date in the following format: "yyyy-mm-dd" [FlagfitIllegalNoExpireParam]
            @FlagType.Experiment(
            ^
        2 errors, 0 warnings
        """.trimIndent()
      )
  }

  override fun getDetector(): Detector = DeadlineExpiredDetector()

  override fun getIssues(): MutableList<Issue> {
    return mutableListOf(
      ISSUE_DEADLINE_EXPIRED,
      ISSUE_DEADLINE_SOON,
      ISSUE_ILLEGAL_NO_EXPIRE_PARAM
    )
  }
}
