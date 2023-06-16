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
import com.android.tools.lint.detector.api.StringOption
import org.jetbrains.uast.UElement
import org.jetbrains.uast.kotlin.KotlinUMethod
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

  @Suppress("NewApi", "UnstableApiUsage")
  override fun visitAnnotationUsage(
    context: JavaContext,
    element: UElement,
    annotationInfo: AnnotationInfo,
    usageInfo: AnnotationUsageInfo,
  ) {
    val timeZoneId = TIME_ZONE.getValue(context)
    val currentTime = CURRENT_TIME.getValue(context)
    val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE.apply {
      if (timeZoneId.isNullOrEmpty()) {
        withZone(ZoneId.systemDefault())
      } else {
        withZone(ZoneId.of(timeZoneId))
      }
    }
    val qualifiedName = annotationInfo.qualifiedName
    val annotationAttributes = annotationInfo.annotation.attributeValues
    val owner = (annotationAttributes.firstOrNull { it.name == "owner" }?.evaluate() as String?)
      ?: ""
    val expiryDate = (annotationAttributes.firstOrNull { it.name == "expiryDate" }
      ?.evaluate() as String?) ?: ""
    if (owner == "OWNER_NOT_DEFINED" || expiryDate == "EXPIRY_DATE_NOT_DEFINED") return
    if (qualifiedName == "tv.abema.flagfit.FlagType.Ops" && expiryDate.isEmpty()
      || qualifiedName == "tv.abema.flagfit.FlagType.Permission" && expiryDate.isEmpty()) return
    val currentLocalDate = if (currentTime.isNullOrEmpty()) {
      LocalDate.now()
    } else {
      LocalDate.parse(currentTime, dateTimeFormatter)
    }
    val expiryLocalDate = LocalDate.parse(expiryDate, dateTimeFormatter)
    val soonExpiryLocalDate = expiryLocalDate.minusDays(7)
    val uastParent = (element.uastParent as? KotlinUMethod) ?: return
    val methodName = uastParent.name
    val key = uastParent.annotations
      .first {
        it.qualifiedName == "tv.abema.flagfit.annotation.BooleanFlag" ||
          it.qualifiedName == "tv.abema.flagfit.annotation.VariationFlag"
      }
      .parameterList.attributes.first { it.name == "key" }.value?.text
    if (currentLocalDate.isAfter(soonExpiryLocalDate)) {
      val name = annotationInfo.qualifiedName.substringAfterLast('.')
      val location = context.getLocation(element)
      if (currentLocalDate.isAfter(expiryLocalDate)) {
        val message = "The @FlagType.$name created by `owner: $owner` has expired!\n" +
          "Please consider deleting @FlagType.$name as the expiration date has passed on $expiryDate.\n" +
          "The flag of `key: ${key}` is used in the $methodName function.\n"

        context.report(ISSUE_DEADLINE_EXPIRED, element, location, message)
      } else {
        val message = "The @FlagType.$name `owner: $owner` will expire soon!\n" +
          "Please consider deleting @FlagType.$name as the expiry date of $expiryDate is scheduled to pass within a week." +
          "The flag of `key: ${key}` is used in the $methodName function.\n"

        context.report(ISSUE_DEADLINE_SOON, element, location, message)
      }
    }
  }

  companion object {
    val TIME_ZONE = StringOption("timeZone", "Your current time zone")
    val CURRENT_TIME = StringOption(
      "currentTime",
      "It's your time now, but this option is only for testing purposes."
    )
    val ISSUE_DEADLINE_EXPIRED = Issue.create(
      id = "FlagfitDeadlineExpired",
      briefDescription = "FlagType annotation's date is in the past!",
      explanation = "The date provided in @FlagType annotation has already passed...",
      category = Category.PRODUCTIVITY,
      priority = 6,
      severity = Severity.WARNING,
      implementation = Implementation(
        DeadlineExpiredDetector::class.java,
        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
      )
    ).setOptions(listOf(TIME_ZONE, CURRENT_TIME))
    val ISSUE_DEADLINE_SOON = Issue.create(
      id = "FlagfitDeadlineSoon",
      briefDescription = "FlagType annotations will expire soon!",
      explanation = "The one annotated with @FlagType will expire in less than a week...",
      category = Category.PRODUCTIVITY,
      priority = 2,
      severity = Severity.WARNING,
      implementation = Implementation(
        DeadlineExpiredDetector::class.java,
        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
      )
    ).setOptions(listOf(TIME_ZONE, CURRENT_TIME))
  }
}
