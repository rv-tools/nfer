package test.test5_alg3

import nfer._
import test._

/** *********************/
/* Testing Algorithm 3 */
/** *********************/

object Test5 extends UnitTest {
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

    // ==================
    // Testing BOOT
    // ==================

    verify("BOOT")(
      P(
        Interval("BOOT", 1000, 2000, Map("count" -> 1)),
        Interval("BOOT", 3000, 4000, Map("count" -> 2)),
        Interval("BOOT", 5000, 6000, Map("count" -> 3))),
      P(
        Interval("BOOT_S", 5000, 5000, Map("count" -> 3))),
      P(
        Interval("BOOT_E", 6000, 6000, Map())))

    // ==================
    // Testing DBOOT
    // ==================

    verify("DBOOT")(
      P(
      ),
      P(
        Interval("BOOT", 5000, 6000, Map("count" -> 3))),
      P(
        Interval("BOOT", 5000, 6000, Map("count" -> 3))))
  }
}