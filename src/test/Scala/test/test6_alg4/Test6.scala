package test.test6_alg4

import nfer._
import test._

/** *********************/
/* Testing Algorithm 4 */
/** *********************/

object Test6 extends UnitTest {
  def main(args: Array[String]): Unit = {
    implicit val nfer = new Nfer().dotDir("results/dotfiles").delay(false)

    val specText =
      """
         BOOT :- BOOT_S before BOOT_E
           map {count -> BOOT_S.count}

         DBOOT :- b1:BOOT before b2:BOOT
           where b2.begin - b1.end <= 5000
           map {count -> b2.count}
      """

    nfer.specText(specText)

    nfer.submit("BOOT_S", 1000, "count" -> 1)
    nfer.submit("BOOT_E", 2000)

    nfer.submit("BOOT_S", 3000, "count" -> 2)
    nfer.submit("BOOT_E", 4000)

    nfer.submit("BOOT_S", 5000, "count" -> 3)
    nfer.submit("BOOT_E", 6000)

    nfer.submit("BOOT_S", 7000, "count" -> 4)
    nfer.submit("BOOT_E", 8000)

    nfer.submit("BOOT_S", 9000, "count" -> 5)
    nfer.submit("BOOT_E", 10000)

    nfer.submit("BOOT_S", 11000, "count" -> 6)
    nfer.submit("BOOT_E", 12000)

    nfer.submit("BOOT_S", 4500, "count" -> 7)
    nfer.submit("BOOT_E", 6501)

    verify("BOOT")(
      P(
        Interval("BOOT", 5000, 6000, Map("count" -> 3)),
        Interval("BOOT", 9000, 10000, Map("count" -> 5)),
        Interval("BOOT", 1000, 2000, Map("count" -> 1)),
        Interval("BOOT", 3000, 4000, Map("count" -> 2)),
        Interval("BOOT", 7000, 8000, Map("count" -> 4)),
        Interval("BOOT", 11000, 12000, Map("count" -> 6))),
      P(
        Interval("BOOT_S", 11000, 11000, Map("count" -> 6))),
      P(
        Interval("BOOT_E", 10000, 10000, Map()),
        Interval("BOOT_E", 12000, 12000, Map()),
        Interval("BOOT_E", 6501, 6501, Map())))

    verify("DBOOT")(
      P(
        Interval("DBOOT", 9000, 12000, Map("count" -> 6)),
        Interval("DBOOT", 5000, 8000, Map("count" -> 4)),
        Interval("DBOOT", 3000, 6000, Map("count" -> 3)),
        Interval("DBOOT", 7000, 10000, Map("count" -> 5)),
        Interval("DBOOT", 1000, 4000, Map("count" -> 2))),
      P(
        Interval("BOOT", 11000, 12000, Map("count" -> 6))),
      P(
        Interval("BOOT", 11000, 12000, Map("count" -> 6))))
  }
}

