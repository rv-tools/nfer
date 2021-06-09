package test.test9_deep_tree

import nfer._
import test._

/** ********************/
/* Testing A Deep Tree */
/** ********************/

object Test9 extends UnitTest {
  def main(args: Array[String]): Unit = {
    implicit val nfer = new Nfer().dotDir("results/dotfiles").delay(true)
    nfer.PRINT_SUBMIT = true

    val specText1 =
      """
        BOOT :- BOOT_S before BOOT_E map {count -> BOOT_S.count}

        DBOOT :- b1:BOOT before b2:BOOT
          where b2.begin - b1.end <= 300 map {count -> b2.count}

        RISK :- DOWNLINK during DBOOT map {count -> DBOOT.count}
      """

    val specText2 =
      """
        BOOT :- BOOT_S before BOOT_E map {count -> BOOT_S.count}

        RISK :- DOWNLINK during (b1:BOOT before b2:BOOT)
          where b2.begin - b1.end <= 300
          map {count -> b2.count}
      """

    nfer.specText(specText2)

    nfer.submit("BOOT_S", 100, "count" -> 1)
    nfer.submit("BOOT_E", 200)
    nfer.submit("DOWNLINK", 300)
    nfer.submit("BOOT_S", 400, "count" -> 2)
    nfer.submit("BOOT_E", 500)
  }
}