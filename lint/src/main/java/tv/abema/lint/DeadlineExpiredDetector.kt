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
import java.time.ZoneId
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
    val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.of("Asia/Tokyo"))
    val annotationAttributes = annotationInfo.annotation.attributeValues
    val author = annotationAttributes[0].evaluate().toString()
    if (annotationAttributes.size == 2) return
    val expiryDateString = annotationAttributes[2].evaluate().toString()
    val currentLocalDate = if (annotationAttributes.size == 4) {
      LocalDate.parse(
        annotationAttributes[3].evaluate().toString(),
        dateTimeFormatter
      )
    } else {
      LocalDate.now()
    }
    val expiryLocalDate = LocalDate.parse(expiryDateString, dateTimeFormatter)
    val soonExpiryLocalDate = expiryLocalDate.minusDays(7)
    if (currentLocalDate.isAfter(soonExpiryLocalDate)) {
      val name = annotationInfo.qualifiedName.substringAfterLast('.')
      val location = context.getLocation(element)
      if (currentLocalDate.isAfter(expiryLocalDate)) {
        val message = "The @FlagType.$name created by $author has expired!\n" +
          "Please consider deleting @FlagType.$name as the expiration date has passed on $expiryDateString."
        context.report(ISSUE_DEADLINE_EXPIRED, element, location, message)
      } else {
        val message = "The @FlagType.$name $author will expire soon!\n" +
          "Please consider deleting @FlagType.$name as the expiry date of $expiryDateString is scheduled to pass within a week."
        context.report(ISSUE_DEADLINE_SOON, element, location, message)
      }
    }
  }

  companion object {
    val ISSUE_DEADLINE_EXPIRED = Issue.create(
      id = "DeadlineExpired",
      briefDescription = "FlagType annotation's date is in the past!",
      explanation = "The date provided in @FlagType annotation has already passed...",
      category = Category.PRODUCTIVITY,
      priority = 6,
      severity = Severity.WARNING,
      implementation = Implementation(
        DeadlineExpiredDetector::class.java,
        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
      )
    )
    val ISSUE_DEADLINE_SOON = Issue.create(
      id = "DeadlineSoon",
      briefDescription = "FlagType annotations will expire soon!",
      explanation = "The one annotated with @FlagType will expire in less than a week...",
      category = Category.PRODUCTIVITY,
      priority = 2,
      severity = Severity.WARNING,
      implementation = Implementation(
        DeadlineExpiredDetector::class.java,
        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
      )
    )
  }
}
