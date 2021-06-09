package test.test17_monami_2000

import nfer._
import nfer.util.JsonReader
import test._

import scala.collection.mutable.ListBuffer
import scala.io.Source

/** ********************/
/* nfer against monami */
/** ********************/

object Test17 extends UnitTest {
  def main(args: Array[String]): Unit = {
    implicit val nfer = new Nfer()
    nfer.setMinimality(false)
    nfer.PRINT_SUBMIT = false

    // MonAmi:
    //   exist A, B, C, D, E .
    //     A('Data1') & B('Data2') & C('Data3') &
    //     A < B & B < C &  & D i E

    val spec =
      """
      IVAL :- BEGIN before END
        where BEGIN.interval = END.interval
        map {interval -> BEGIN.interval, data -> BEGIN.data}

      ORDER :- i1:IVAL before (i2:IVAL before i3:IVAL)
        where i1.data = "Data1" & i2.data = "Data2" & i3.data = "Data3"
        map {data1 -> i1.data, data2 -> i2.data, data3 -> i3.data}

      NESTED :- i1:IVAL during i2:IVAL
        where i1.interval != i2.interval
        map {dataInner -> i1.data, dataOuter -> i2.data}

      FOUND :- ORDER also NESTED
        map {data1 -> ORDER.data1, data2 -> ORDER.data2, data3 -> ORDER.data3,
             dataInner -> NESTED.dataInner, dataOuter -> NESTED.dataOuter}
      """

    nfer.specText(spec)

    val trace = new JsonReader().fromFile("src/test/Scala/test/test17_monami_2000/trace.json")

    nfer.time {
      nfer.submit(trace.toList)
    }
    nfer.printIntervals(false)
  }
}


