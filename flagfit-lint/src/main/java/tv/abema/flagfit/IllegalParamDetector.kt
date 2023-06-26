package tv.abema.flagfit

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
import java.util.EnumSet

class IllegalParamDetector : Detector(), SourceCodeScanner {

  override fun applicableAnnotations(): List<String> {
    return listOf(
      AnnotationPackagePath.PACKAGE_PATH_WIP,
      AnnotationPackagePath.PACKAGE_PATH_EXPERIMENT,
      AnnotationPackagePath.PACKAGE_PATH_OPS,
      AnnotationPackagePath.PACKAGE_PATH_PERMISSION,
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
    val qualifiedName = annotationInfo.qualifiedName
    val location = context.getLocation(element)
    val annotationAttributes = annotationInfo.annotation.attributeValues
    val expiryDate = (annotationAttributes.firstOrNull { it.name == "expiryDate" }
      ?.evaluate() as String?) ?: ""
    if (expiryDate == FlagType.EXPIRY_DATE_INFINITE) {
      when (qualifiedName) {
        AnnotationPackagePath.PACKAGE_PATH_WIP, AnnotationPackagePath.PACKAGE_PATH_EXPERIMENT -> {
          val message = "`NO_EXPIRE_DATE` cannot be set for the expireDate of `@FlagType.WorkInProgress` and `@FlagType.Experiment`.\n" +
            "Please set the expiration date in the following format: \"yyyy-mm-dd\""
          context.report(ISSUE_ILLEGAL_NO_EXPIRE_PARAM, element, location, message)
          return
        }

        AnnotationPackagePath.PACKAGE_PATH_OPS, AnnotationPackagePath.PACKAGE_PATH_PERMISSION -> return
      }
    }
  }

  companion object {
    val ISSUE_ILLEGAL_NO_EXPIRE_PARAM = Issue.create(
      id = "FlagfitIllegalNoExpireParam",
      briefDescription = "The argument of expireDate is illigal.",
      explanation = "Do not set NO_EXPIRE_DATE for @FlagType.WorkInProgress and @FlagType.Experiment...",
      category = Category.PRODUCTIVITY,
      priority = 4,
      severity = Severity.ERROR,
      implementation = Implementation(
        IllegalParamDetector::class.java,
        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
      )
    )
  }
}
