package test.test18_monami_experiments

import nfer._
import nfer.util.JsonReader
import test._

/** ********************/
/* nfer against monami */
/** ********************/

object Test18 extends UnitTest {
  def main(args: Array[String]): Unit = {
    implicit val nfer = new Nfer()
    nfer.setMinimality(false)
    nfer.PRINT_SUBMIT = false

    // MonAmi:
    // exist A, B . A < B & B("data3")

    val spec =
      """
      IVAL :- BEGIN before END
        where BEGIN.interval = END.interval
        map {interval -> BEGIN.interval, data -> BEGIN.data}

      FOUND :- i1:IVAL before i2:IVAL where i2.data = "Data3"
      """

    nfer.specText(spec)

    val trace = new JsonReader().fromFile("src/test/Scala/test/test18_monami_experiments/trace.json")

    nfer.time {
      nfer.submit(trace.toList)
    }
    nfer.printIntervals(true)
  }
}


