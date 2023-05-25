package tv.abema.flagfit

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object Extension {
  fun String.toLocalDate(): LocalDate {
    val dateString = this
    val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    return LocalDate.parse(dateString, dateFormat)
  }
}
