package test

import nfer.{Interval, Nfer, Sclk}

trait Check
case object NoCheck extends Check
case class PartialCheck(intervals: Set[Interval]) extends Check
case class FullCheck(intervals: Set[Interval]) extends Check


class UnitTest {
  /**
    * Convenience function creation an interval from an atomic event
    * having no duration occurring in the trace.
    *
    * @param name the name of the event.
    * @param time the time of the event.
    * @param data the data the event carries.
    * @return an interval with the start and end time being the time of the event.
    */

  def event(name: String, time: Sclk, data: (String, Any)*) : Interval = {
    Interval(name, time, time, data.toMap)
  }

  def N: Check = NoCheck
  def P(intervals: Interval*): Check = PartialCheck(intervals.toSet)
  def F(intervals: Interval*): Check = FullCheck(intervals.toSet)

  def checkPair(nodeNr: Int, intervals: Set[Interval], check : Check): Unit = {
    val errorMessage =
      s"""
         |++++++++++++++
         |Node number $nodeNr
         |
         |${intervals.mkString("\n")}
         |do not match
         |$check
         |++++++++++++++
       """.stripMargin
    check match {
      case NoCheck =>
      case PartialCheck(intervalsSpec) =>
        assert(intervalsSpec subsetOf intervals, errorMessage)
      case FullCheck(intervalsSpec) =>
        assert(intervals == intervalsSpec, errorMessage)
    }
  }

  def verify(ruleName: String)(checks: Check*)(implicit nfer: Nfer): Unit = {
    println()
    println("// ==================")
    println(s"// Testing $ruleName")
    println("// ==================")
    println()
    val intervalSets = nfer.getIntervalsInTree(ruleName)
    printIntervalSets(ruleName, intervalSets)
    val checkPairs = intervalSets zip checks.toList
    var nodeNr: Int = 0
    for ((intervalSet,spec) <- checkPairs) {
      nodeNr += 1
      checkPair(nodeNr, intervalSet,spec)
    }
  }

  def quote(str: String) = "\"" + str + "\""

  def printIntervalSets(ruleName: String, intervals: List[Set[Interval]]): Unit = {
    var checks : List[String] = Nil
    for (intervalSet <- intervals) {
      var str: String = "  P(\n"
      str += intervalSet.map(_.toStringForTest).mkString("    ",",\n    ","")
      str += ")"
      checks ++= List(str)
    }
    var result = s"verify(${quote(ruleName)})(\n"
    result += checks.mkString(",\n")
    result += ")"
    println(result)
  }
}