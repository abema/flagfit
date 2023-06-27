package tv.abema.flagfit

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import tv.abema.flagfit.FlagTypeExpiryDateIllegalParamDetector.Companion.ISSUE_ILLEGAL_DATE
import tv.abema.flagfit.FlagTypeExpiryDateIllegalParamDetector.Companion.ISSUE_ILLEGAL_NO_EXPIRE_PARAM

@RunWith(JUnit4::class)
class FlagTypeExpiryDateIllegalParamDetectorText : LintDetectorTest() {

  private lateinit var stabBooleanFlag: TestFile
  private lateinit var stabFlagType: TestFile

  @Before
  fun before() {
    stabBooleanFlag = KotlinStabFiles.stabBooleanFlag
    stabFlagType = KotlinStabFiles.stabFlagType
  }

  @Test
  fun testIllegalNoExpireParamWarning() {
    lint()
      .files(
        stabBooleanFlag,
        stabFlagType,
        kotlin(
          """
          package foo
          import tv.abema.flagfit.FlagType
          import tv.abema.flagfit.annotation.BooleanFlag
          import tv.abema.flagfit.FlagType.Companion.OWNER_NOT_DEFINED
          import tv.abema.flagfit.FlagType.Companion.EXPIRY_DATE_INFINITE
          
          interface Example {
              @BooleanFlag(
                key = "new-wip-awesome-feature",
                defaultValue = false
              )
              @FlagType.WorkInProgress(
                owner = "Hoge Fuga",
                expiryDate = EXPIRY_DATE_INFINITE
              )
              fun awesomeWipFeatureEnabled(): Boolean
              @BooleanFlag(
                key = "new-experiment-awesome-feature",
                defaultValue = false
              )
              @FlagType.Experiment(
                owner = OWNER_NOT_DEFINED,
                expiryDate = EXPIRY_DATE_INFINITE
              )
              fun awesomeExperimentFeatureEnabled(): Boolean
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

  @Test
  fun testNoExpireParamNoWarning() {
    lint()
      .files(
        stabBooleanFlag,
        stabFlagType,
        kotlin(
          """
          package foo
          import tv.abema.flagfit.FlagType
          import tv.abema.flagfit.annotation.BooleanFlag
          import tv.abema.flagfit.FlagType.Companion.OWNER_NOT_DEFINED
          import tv.abema.flagfit.FlagType.Companion.EXPIRY_DATE_INFINITE
          
          interface Example {
              @BooleanFlag(
                key = "new-ops-awesome-feature",
                defaultValue = false
              )
              @FlagType.Ops(
                owner = "Hoge Fuga",
                expiryDate = EXPIRY_DATE_INFINITE
              )
              fun awesomeOpsFeatureEnabled(): Boolean
              @BooleanFlag(
                key = "new-permission-awesome-feature",
                defaultValue = false
              )
              @FlagType.Permission(
                owner = OWNER_NOT_DEFINED,
                expiryDate = EXPIRY_DATE_INFINITE
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
  fun testIllegalExpireDateFormatWarning() {
    lint()
      .files(
        stabBooleanFlag,
        stabFlagType,
        kotlin(
          """
          package foo
          import tv.abema.flagfit.FlagType
          import tv.abema.flagfit.annotation.BooleanFlag
          import tv.abema.flagfit.FlagType.Companion.OWNER_NOT_DEFINED
          import tv.abema.flagfit.FlagType.Companion.EXPIRY_DATE_INFINITE
          
          interface Example {
              @BooleanFlag(
                key = "new-awesome-feature",
                defaultValue = false
              )
              @FlagType.Ops(
                owner = "Hoge Fuga",
                expiryDate = "2023-12-100"
              )
              fun awesomeFeatureEnabled(): Boolean
          }
          """.trimIndent()
        )
      )
      .issues(*issues.toTypedArray())
      .allowMissingSdk()
      .run()
      .expect(
        """
        src/foo/Example.kt:12: Error: The value of expireDate is not in the correct date format.
        Please set the expiration date in the following format: "yyyy-mm-dd" [FlagfitIllegalDate]
            @FlagType.Ops(
            ^
        1 errors, 0 warnings
        """.trimIndent()
      )
  }

  @Test
  fun testExpireDateFormatNoWarning() {
    lint()
      .files(
        stabBooleanFlag,
        stabFlagType,
        kotlin(
          """
          package foo
          import tv.abema.flagfit.FlagType
          import tv.abema.flagfit.annotation.BooleanFlag
          import tv.abema.flagfit.FlagType.Companion.OWNER_NOT_DEFINED
          import tv.abema.flagfit.FlagType.Companion.EXPIRY_DATE_INFINITE
          
          interface Example {
              @BooleanFlag(
                key = "new-awesome-feature",
                defaultValue = false
              )
              @FlagType.WorkInProgress(
                owner = "Hoge Fuga",
                expiryDate = "2023-12-10"
              )
              fun awesomeFeatureEnabled(): Boolean
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
  fun testDeprecatedFlagTypeNoWarning() {
    lint()
      .files(
        stabBooleanFlag,
        stabFlagType,
        kotlin(
          """
          package foo
          import tv.abema.flagfit.FlagType
          import tv.abema.flagfit.annotation.BooleanFlag
          import tv.abema.flagfit.FlagType.Companion.OWNER_NOT_DEFINED
          import tv.abema.flagfit.FlagType.Companion.EXPIRY_DATE_NOT_DEFINED
          
          interface Example {
              @BooleanFlag(
                key = "new-awesome-feature",
                defaultValue = false
              )
              @FlagType.Ops(
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

  override fun getDetector(): Detector = FlagTypeExpiryDateIllegalParamDetector()

  override fun getIssues(): MutableList<Issue> {
    return mutableListOf(
      ISSUE_ILLEGAL_NO_EXPIRE_PARAM,
      ISSUE_ILLEGAL_DATE
    )
  }
}
