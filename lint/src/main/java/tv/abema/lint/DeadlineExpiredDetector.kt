package tv.abema.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULiteralExpression
import java.time.LocalDate
import java.util.Collections
import java.util.EnumSet

class DeadlineExpiredDetector: Detector(), Detector.UastScanner {
  override fun getApplicableUastTypes(): List<Class<out UElement>>? {
    return Collections.singletonList(UClass::class.java)
  }

  override fun createUastHandler(context: JavaContext): UElementHandler {
    return object : UElementHandler() {
      override fun visitClass(node: UClass) {
        val annotations = node.annotations
        for (annotation in annotations) {
          if (annotation.qualifiedName == "tv.abema.flagfit.FragType.Ops") {
            val expireDateAttribute = annotation.findAttributeValue("expireDate")
            if (expireDateAttribute is ULiteralExpression) {
              val expireDateString = expireDateAttribute.value as String
              val expireDate = LocalDate.parse(expireDateString) // Assumes the date is in ISO-8601 format.
              val currentDate = LocalDate.now()
              if (currentDate.isAfter(expireDate)) {
                context.report(
                  issue = ISSUE_DEADLINE_EXPIRED,
                  location = context.getLocation(annotation),
                  message = "The deadline in @Ops annotation has already passed"
                )
              }
            }
          }
        }
      }
    }
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
