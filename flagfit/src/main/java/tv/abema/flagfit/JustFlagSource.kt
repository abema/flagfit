package tv.abema.flagfit

sealed class JustFlagSource {
  object True : SuspendableBooleanFlagSource, BlockingBooleanFlagSource, JustFlagSource() {
    override suspend fun fetch(key: String, defaultValue: Boolean, env: Map<String, Any>): Boolean {
      return true
    }

    override fun get(key: String, defaultValue: Boolean, env: Map<String, Any>): Boolean {
      return true
    }
  }

  object False : SuspendableBooleanFlagSource, BlockingBooleanFlagSource, JustFlagSource() {
    override suspend fun fetch(key: String, defaultValue: Boolean, env: Map<String, Any>): Boolean {
      return false
    }

    override fun get(key: String, defaultValue: Boolean, env: Map<String, Any>): Boolean {
      return false
    }
  }

  data class StringSource(
    val value: String
  ) : SuspendableStringFlagSource, BlockingStringFlagSource, JustFlagSource() {
    override suspend fun fetch(
      key: String,
      defaultValue: String,
      env: Map<String, Any>
    ): String {
      return value
    }

    override fun get(
      key: String,
      defaultValue: String,
      env: Map<String, Any>
    ): String {
      return value
    }
  }
}
