package test.test19_monami_evaluation

import nfer._
import nfer.util.JsonReader
import test._

/** ********************/
/* nfer against monami */
/** ********************/

object Test19 extends UnitTest {
  val dir = "src/test/Scala/test/test19_monami_evaluation/"

  val specs = List(
    "spec1",
    "spec2",
    // "spec3",
    "spec4",
    "spec5"
  )
  val traces = List(
    "trace1000.json",
    "trace2000.json",
    "trace4000.json",
    "trace8000.json",
    "trace16000.json"
  )

  def test(): Unit = {
    test(specs, traces)
  }

  def test(specs: List[String], traces: List[String]): Unit = {
    for (spec <- specs; trace <- traces) {
      test(spec, trace)
    }
  }

  def test(specName: String, traceName: String): Unit = {
    test(specName, traceName, false)
    test(specName, traceName, true)
  }

  def test(specName: String, traceName: String, minimality : Boolean): Unit = {
    println(s"\n##### $specName applied to $traceName with minimality = $minimality #####\n")
    implicit val nfer = new Nfer()
    nfer.setMinimality(minimality)
    nfer.PRINT_SUBMIT = false
    nfer.specFile(dir + specName)
    val trace = new JsonReader().fromFile(dir + traceName)
    nfer.time { nfer.submit(trace.toList) }
    nfer.printIntervals(true)
  }

  def main(args: Array[String]): Unit = {
    test("spec9.1.nfer","trace_9.1.json", false)
  }
}


