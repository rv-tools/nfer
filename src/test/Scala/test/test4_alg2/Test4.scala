package test.test4_alg2

import nfer._
import test._

/** *********************/
/* Testing Algorithm 2 */
/** *********************/

object Test4 extends UnitTest {
  def main(args: Array[String]): Unit = {
    implicit val nfer = new Nfer().dotDir("results/dotfiles").delay(true)

    val specText1 =
      """
         BOOT :- BOOT_S before BOOT_E
           where BOOT_E.begin - BOOT_S.end <= 1000
           map {count -> BOOT_S.count}

         DBOOT :- b1:BOOT before b2:BOOT
           where b2.begin - b1.end <= 5000
           map {count -> b2.count}
      """

    val specText2 =
      """
         DBOOT :- (BOOT_S before BOOT_E) before (BOOT_S before BOOT_E)
      """

    println("begin")

    nfer.specText(specText1)

    println("step 0")

    nfer.submit("BOOT_S", 1000, "count" -> 1)

    println("step 0.5")

    nfer.submit("BOOT_E", 2001)

    println("step 1")

    nfer.submit("BOOT_S", 3000, "count" -> 2)
    nfer.submit("BOOT_E", 4000)

    println("step 2")

    nfer.submit("BOOT_S", 5000, "count" -> 3)
    nfer.submit("BOOT_E", 6001)

    println("step 3")

    nfer.submit("BOOT_S", 7002, "count" -> 4)
    nfer.submit("BOOT_E", 8000)

    println("step 4")

    nfer.submit("BOOT_S", 9000, "count" -> 5)
    nfer.submit("BOOT_E", 10000)

    println("step 5")

    nfer.submit("BOOT_S", 11000, "count" -> 6)
    nfer.submit("BOOT_E", 12000)

    println("end")

    // ==================
    // Testing BOOT
    // ==================

    verify("BOOT")(
      P(
        Interval("BOOT", 3000, 4000, Map("count" -> 2)),
        Interval("BOOT", 7002, 8000, Map("count" -> 4)),
        Interval("BOOT", 9000, 10000, Map("count" -> 5)),
        Interval("BOOT", 11000, 12000, Map("count" -> 6))),
      P(
        Interval("BOOT_S", 1000, 1000, Map("count" -> 1)),
        Interval("BOOT_S", 5000, 5000, Map("count" -> 3))),
      P(
        Interval("BOOT_E", 2001, 2001, Map()),
        Interval("BOOT_E", 6001, 6001, Map())))

    // ==================
    // Testing DBOOT
    // ==================

    verify("DBOOT")(
      P(
        Interval("DBOOT", 3000, 8000, Map("count" -> 4)),
        Interval("DBOOT", 7002, 10000, Map("count" -> 5)),
        Interval("DBOOT", 9000, 12000, Map("count" -> 6))),
      P(
        Interval("BOOT", 11000, 12000, Map("count" -> 6))),
      P(
        Interval("BOOT", 3000, 4000, Map("count" -> 2))))
  }
}