package test.test1_alg1

import nfer._
import test._

object Test1 extends UnitTest {
  def main(args: Array[String]): Unit = {
    implicit val nfer = new Nfer().dotDir("results/dotfiles").delay(false)

    // Same spec in two formats, one using just rules and one using modules:

    val spec1Text = // rules at outermost level
      """

         BOOT :- BOOT_S before BOOT_E where BOOT_E.begin - BOOT_S.end <= 5000 map {count -> BOOT_S.count}
         DBOOT :- b1:BOOT before b2:BOOT where b2.begin - b1.end <= 10000 map {count -> b1.count}
         RISK :- DOWNLINK during DBOOT map {count -> DBOOT.count}

      """

    val spec2Text = // modules at outmost level
      """

        module Booting {
          BOOT :- BOOT_S before BOOT_E where BOOT_E.begin - BOOT_S.end <= 5000 map {count -> BOOT_S.count}
        }

        module DoubleBooting {
          import Booting;

          DBOOT :- b1:BOOT before b2:BOOT where b2.begin - b1.end <= 10000 map {count -> b1.count}
        }

        module Risking {
          import DoubleBooting;

          RISK :- DOWNLINK during DBOOT map {count -> DBOOT.count}
        }

      """

    val spec3Text =
      """
        BOOT :- BOOT_S before BOOT_E map {count -> BOOT_S.count}
        DBOOT :- b1:BOOT before b2:BOOT where b2.begin - b1.end <= 300 map {count -> b2.count}
        RISK :- DOWNLINK during DBOOT map {count -> DBOOT.count}
      """

    // Same specs but from files:

    nfer.specText(spec3Text)

    nfer.submit("BOOT_S", 1000, "count" -> 42)
    nfer.submit("BOOT_E", 2000)
    nfer.submit("BOOT_S", 3000, "count" -> 43)
    nfer.submit("DOWNLINK", 4000)
    nfer.submit("BOOT_E", 5000)

    verify("RISK")()
  }
}

