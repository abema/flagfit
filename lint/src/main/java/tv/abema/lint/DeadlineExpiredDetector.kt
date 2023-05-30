package tv.abema.lint

import com.android.tools.lint.detector.api.AnnotationInfo
import com.android.tools.lint.detector.api.AnnotationUsageInfo
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UElement
import java.util.Collections
import java.util.EnumSet

class DeadlineExpiredDetector : Detector(), Detector.UastScanner {

  override fun applicableAnnotations(): List<String> {
    return listOf(
      "tv.abema.fragfit.annotation.BooleanFlag",
      "tv.abema.flagfit.FlagType"
    )
  }

  override fun visitAnnotationUsage(
    context: JavaContext,
    element: UElement,
    annotationInfo: AnnotationInfo,
    usageInfo: AnnotationUsageInfo,
  ) {
    val name = annotationInfo.qualifiedName.substringAfterLast('.')
    val message = "`${usageInfo.type.name}` usage associated with " +
      "`@$name` on ${annotationInfo.origin}"
    val location = context.getLocation(element)
    context.report(ISSUE_DEADLINE_EXPIRED, element, location, message)
  }

  companion object {
    private const val DEADLINE_EXPIRED_CLASS = "tv.abema.lint.DeadlineExpiredDetector"
    val ISSUE_DEADLINE_EXPIRED = Issue.create(
      id = "DeadlineExpired",
      briefDescription = "Ops annotation's date is in the past",
      explanation = "The date provided in @Ops annotation has already passed...",
      category = Category.CORRECTNESS,
      priority = 6,
      severity = Severity.WARNING,
      implementation = Implementation(
        DeadlineExpiredDetector::class.java,
        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
      )
    )
    val issue = ISSUE_DEADLINE_EXPIRED
  }
}
