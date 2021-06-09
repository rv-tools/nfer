package nfer

import nfer.util.JsonReader

/** ********************/
/* nfer against monami */
/** ********************/

object Main {
  def main(args: Array[String]): Unit = {
    val specName = args(0)
    val traceName = args(1)
    var minimality : Boolean = true
    var printIntervals : Boolean = true
    if (args.size > 4) {
      assert(false, "format is: nfer <specFile> <traceFile> [true|false] [true|false]")
    }
    if (args.size >= 3) {
      val minimalityParameter = args(2)
      minimalityParameter match {
        case "true" =>  minimality = true
        case "false" => minimality = false
        case _ => assert(false, s"wrong parameter: $minimalityParameter")
      }
    }
    if (args.size == 4) {
      val printIntervalsParameter = args(3)
      printIntervalsParameter match {
        case "true" =>  printIntervals = true
        case "false" => printIntervals = false
        case _ => assert(false, s"wrong parameter: $printIntervalsParameter")
      }
    }
    println(s"\n##### $specName applied to $traceName with minimality = $minimality #####\n")
    implicit val nfer = new Nfer()
    nfer.setMinimality(minimality)
    nfer.specFile(specName)
    val trace = new JsonReader().fromFile(traceName)
    nfer.time { nfer.submit(trace.toList) }
    nfer.printIntervals(printIntervals)
  }
}


