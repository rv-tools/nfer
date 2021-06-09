package test.test3_alg1

import nfer._
import test._

class Test3 extends UnitTest {
  def test1() {
    implicit val nfer = new Nfer().dotDir("results/dotfiles").delay(false)

    def timeOk(time1: Int, time2: Int, diff: Int): Boolean = time2 - time1 <= diff

    def add(value1: Int, value2: Int): Int = value1 + value2

    nfer.addOperation("timeOk")(
      new Operation {
        override def apply(args: Any*): Any = {
          val time1: Int = args(0).asInstanceOf[Int]
          val time2: Int = args(1).asInstanceOf[Int]
          val diff: Int = args(2).asInstanceOf[Int]
          timeOk(time1, time2, diff)
        }
      })

    nfer.addOperation("add")(
      new Operation {
        override def apply(args: Any*): Any = {
          val value1: Int = args(0).asInstanceOf[Int]
          val value2: Int = args(1).asInstanceOf[Int]
          add(value1, value2)
        }
      })

    val specText =
      """
         BOOT :- BOOT_S before BOOT_E
           map {count -> BOOT_S.count}

         DBOOT :- b1:BOOT before b2:BOOT
           where timeOk(b1.end, b2.begin, 5000)
           map {count -> add(b1.count,100)}

         RISK :- DOWNLINK during DBOOT
           map {count -> DBOOT.count}
      """

    nfer.specText(specText)

    nfer.submit("BOOT_S", 1000, "count" -> 42)
    nfer.submit("BOOT_E", 2000)
    nfer.submit("BOOT_S", 3000, "count" -> 43)
    nfer.submit("DOWNLINK", 4000)
    nfer.submit("BOOT_E", 5000)

    // ==================
    // Testing BOOT
    // ==================

    verify("BOOT")(
      P(
        Interval("BOOT", 1000, 2000, Map("count" -> 42)),
        Interval("BOOT", 3000, 5000, Map("count" -> 43))),
      P(
        Interval("BOOT_S", 1000, 1000, Map("count" -> 42)),
        Interval("BOOT_S", 3000, 3000, Map("count" -> 43))),
      P(
        Interval("BOOT_E", 2000, 2000, Map()),
        Interval("BOOT_E", 5000, 5000, Map())))

    // ==================
    // Testing DBOOT
    // ==================

    verify("DBOOT")(
      P(
        Interval("DBOOT", 1000, 5000, Map("count" -> 142))),
      P(
        Interval("BOOT", 1000, 2000, Map("count" -> 42)),
        Interval("BOOT", 3000, 5000, Map("count" -> 43))),
      P(
        Interval("BOOT", 1000, 2000, Map("count" -> 42)),
        Interval("BOOT", 3000, 5000, Map("count" -> 43))))

    // ==================
    // Testing RISK
    // ==================

    verify("RISK")(
      P(
        Interval("RISK", 1000, 5000, Map("count" -> 142))),
      P(
        Interval("DOWNLINK", 4000, 4000, Map())),
      P(
        Interval("DBOOT", 1000, 5000, Map("count" -> 142))))
  }
}