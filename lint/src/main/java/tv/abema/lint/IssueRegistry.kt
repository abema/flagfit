package tv.abema.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

@Suppress("UnstableApiUsage")
internal class IssueRegistry : IssueRegistry() {
  override val api: Int
    get() = CURRENT_API

  override val issues: List<Issue>
    get() = listOf(
      DeadlineExpiredDetector.issue,
    )
}
