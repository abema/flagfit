package tv.abema.flagfit

private const val RAW_DEFAULT_VALUE = ""

enum class BooleanOrDefault(
  val variationId: String
) {
  TRUE("true"),
  FALSE("false"),
  DEFAULT(RAW_DEFAULT_VALUE),
  ;

  companion object : VariationAdapter<BooleanOrDefault>(
    BooleanOrDefault::class
  ) {
    const val DEFAULT_VALUE = RAW_DEFAULT_VALUE

    override fun variationOf(value: String): BooleanOrDefault {
      return values()
        .firstOrNull { type -> value == type.variationId }
        ?: DEFAULT
    }
  }
}
