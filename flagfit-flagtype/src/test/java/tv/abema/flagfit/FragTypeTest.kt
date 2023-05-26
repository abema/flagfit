package tv.abema.flagfit

import org.junit.Test
import tv.abema.flagfit.annotation.BooleanFlag

class FragTypeTest {
  @Test
  @BooleanFlag(
    key = "HOGE_KEY",
    defaultValue = false,
  )
  @FlagType.Ops(
    author = "hoge",
    description = "hoge",
    expiryDate = "2022-12-30"
  )
  fun isTestEnable(): Unit {
    return
  }
}
