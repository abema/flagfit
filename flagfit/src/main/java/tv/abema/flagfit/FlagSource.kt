package tv.abema.flagfit

interface SuspendableBooleanFlagSource : FlagSource {
  suspend fun fetch(
    key: String,
    defaultValue: Boolean,
    env: Map<String, Any>
  ): Boolean
}

interface BlockingBooleanFlagSource : FlagSource {
  fun get(
    key: String,
    defaultValue: Boolean,
    env: Map<String, Any>
  ): Boolean
}

interface SuspendableStringFlagSource : FlagSource {
  suspend fun fetch(
    key: String,
    defaultValue: String,
    env: Map<String, Any>
  ): String
}

interface BlockingStringFlagSource : FlagSource {
  fun get(
    key: String,
    defaultValue: String,
    env: Map<String, Any>
  ): String
}

interface FlagSource
