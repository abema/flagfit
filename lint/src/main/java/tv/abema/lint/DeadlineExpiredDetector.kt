package tv.abema.lint

import com.android.tools.lint.detector.api.AnnotationInfo
import com.android.tools.lint.detector.api.AnnotationUsageInfo
import com.android.tools.lint.detector.api.AnnotationUsageType
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UElement
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.EnumSet

class DeadlineExpiredDetector : Detector(), SourceCodeScanner {

  override fun applicableAnnotations(): List<String> {
    return listOf(
      "tv.abema.flagfit.FlagType.WorkInProgress",
      "tv.abema.flagfit.FlagType.Experiment",
      "tv.abema.flagfit.FlagType.Ops",
      "tv.abema.flagfit.FlagType.Permission",
    )
  }

  override fun isApplicableAnnotationUsage(type: AnnotationUsageType): Boolean {
    return type == AnnotationUsageType.DEFINITION || super.isApplicableAnnotationUsage(type)
  }

  @Suppress("NewApi")
  override fun visitAnnotationUsage(
    context: JavaContext,
    element: UElement,
    annotationInfo: AnnotationInfo,
    usageInfo: AnnotationUsageInfo,
  ) {
    val expiryDateString = annotationInfo.annotation.attributeValues[2].evaluate().toString()
    val expiryDate = LocalDate.parse(expiryDateString, DateTimeFormatter.ISO_LOCAL_DATE)
    val currentDate = LocalDate.now()
    if (currentDate.isAfter(expiryDate)) {
      val name = annotationInfo.qualifiedName.substringAfterLast('.')
      val message = "Your @FlagType.$name has expired!\n" +
        "Ops has expired. Please consider deleting @FlagType.$name"
      val location = context.getLocation(element)
      context.report(ISSUE_DEADLINE_EXPIRED, element, location, message)
    }
  }

  companion object {
    val ISSUE_DEADLINE_EXPIRED = Issue.create(
      id = "DeadlineExpired",
      briefDescription = "Ops annotation's date is in the past",
      explanation = "The date provided in @FlagType annotation has already passed...",
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
