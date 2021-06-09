package nfer.util

import nfer.Interval

import scala.collection.mutable.ListBuffer
import scala.io.Source

/**
  * Class for reading test files from the MonAmi project,
  * for comparing MonAmi with nfer.
  */

class JsonReader {
  def fromFile(fileName: String): ListBuffer[Interval] = {
    val jsonText = new StringBuffer()
    for (line <- Source.fromFile(fileName).getLines) {
      jsonText.append(line + "\n")
    }
    fromText(jsonText)
  }

  def fromText(jsonText: StringBuffer): ListBuffer[Interval] = {
    var currentTime : Int = 0

    def nextTime: Int = {
      currentTime += 1
      currentTime
    }

    val result = new ListBuffer[Interval]
    var dataMap : Map[String, String] = Map()
    val jsonObject = ujson.read(jsonText).asInstanceOf[ujson.Obj]
    // println(s"\n$jsonObject\n")
    val trace = jsonObject.value("execution").asInstanceOf[ujson.Arr].value
    for (event <- trace) {
      val timeStamp = nextTime
      val eventAsArray = event.asInstanceOf[ujson.Arr].value
      val kind = eventAsArray(0).str
      val interval = eventAsArray(1) match {
        case s: ujson.Str => s.str
        case n: ujson.Num => n.num.toInt.toString
      }
      if (kind == "begin") {
        val data = eventAsArray(2) match {
          case s: ujson.Str => s.str
          case n: ujson.Num => n.num.toInt.toString
        }
        dataMap += (interval -> data)
        result.append(Interval("BEGIN", timeStamp, timeStamp, Map("interval" -> interval, "data" -> data)))
      } else {
        val data = dataMap(interval)
        result.append(Interval("END", timeStamp, timeStamp, Map("interval" -> interval)))
      }
    }
    result
  }
}
