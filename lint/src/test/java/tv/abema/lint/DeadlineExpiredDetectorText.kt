package tv.abema.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import tv.abema.lint.DeadlineExpiredDetector.Companion.ISSUE_DEADLINE_EXPIRED

@RunWith(JUnit4::class)
class DeadlineExpiredDetectorText : LintDetectorTest() {
  @Test
  fun usingFlagType() {
    lint()
      .files(
        kotlin(
          """
          |package foo
          |import com.android.annotations.NonNull
          |import tv.abema.flagfit.FlagType
          |import tv.abema.flagfit.annotation.BooleanFlag
          |
          |interface Example {
          |    @NonNull
          |    @BooleanFlag(
          |    key = "new-awesome-wip-feature",
          |      defaultValue = false
          |    )
          |    @FlagType.Ops(
          |      author = "Hoge Fuga",
          |      description = "hogehoge",
          |      expiryDate = "2022-12-30"
          |    )
          |    fun awesomeWipFeatureEnabled(): Boolean
          |}""".trimMargin()
        )
      )
      .issues(*issues.toTypedArray())
      .allowMissingSdk()
      .allowCompilationErrors()
      .run()
      .expect(
        """
        |src/foo/Example.kt:9: Warning: The deadline in @Ops annotation has already passed
        |    @FlagType.Ops(
        |    ~~~~~~~~~~~~~~~~~~~
        |0 errors, 1 warnings""".trimMargin()
      )
  }

  override fun getDetector(): Detector = DeadlineExpiredDetector()

  override fun getIssues(): MutableList<Issue> {
    return mutableListOf(
      ISSUE_DEADLINE_EXPIRED
    )
  }
}
