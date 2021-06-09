package test.generate_nferc_traces

import nfer.{Interval, Sclk}

import java.io._
import nfer.util.JsonReader

/**
  * Generate traces for Sean Kauffman's nfer tool in C (`nfer.io`) from JSON traces.
  */

object Main {
  val DIR = "src/test/Scala/test/test19_monami_evaluation/new-traces"

  def translateFile(traceFile: String): Unit = {
    val jsonFile = s"$DIR/$traceFile.json"
    val nferFile = s"$DIR/$traceFile.csv"
    println(s"$jsonFile -> $nferFile")
    val trace = new JsonReader().fromFile(jsonFile)
    val pw = new PrintWriter(new File(nferFile))
    for (interval <- trace) {
      val Interval(name, begin, end, data) = interval
      // LABEL|TIMESTAMP|MAPKEYS|MAPVALUES
      val time = begin.toInt
      val pairs = data.toList
      val keys = (pairs map (_._1)).mkString(";")
      val values = (pairs map (_._2)).mkString(";")
      var event = s"$name|$time|$keys|$values"
      pw.write(s"$event\n")
    }
    pw.close
  }

  def main(args: Array[String]): Unit = {
    var traceFiles =
      List(
        "trace_1000_property_1",
        "trace_1000_property_2",
        "trace_1000_property_3",
        "trace_1000_property_4",
        "trace_2000_property_1",
        "trace_2000_property_2",
        "trace_2000_property_3",
        "trace_2000_property_4",
        "trace_4000_property_1",
        "trace_4000_property_2",
        "trace_4000_property_3",
        "trace_4000_property_4",
        "trace_8000_property_1",
        "trace_8000_property_2",
        "trace_8000_property_3",
        "trace_8000_property_4",
        "trace_16000_property_1",
        "trace_16000_property_2",
        "trace_16000_property_3",
        "trace_16000_property_4"
      )
    for (traceFile <- traceFiles) {
      translateFile(traceFile)
    }
  }
}



