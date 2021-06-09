package test.test16_dejavu

import java.io.{BufferedWriter, File, FileWriter}

import nfer._
import test._

import scala.io.Source

// From test7 modified to generate logs for DejaVu with time

class FileReader {
  def mkInterval(line: String): Interval = {
    var name : String = null
    var time : Int = 0
    var data : Map[String,Any] = Map()

    val dataArray : Array[String]= line.split("\\|")
    val namePart = dataArray(0)
    time = dataArray(1).toInt
    if (namePart.startsWith("boot")) {
      val nameParts = namePart.split("_")
      name = nameParts(0) + "_" + nameParts(1)
      data += ("value" -> nameParts(2))
    } else {
      name = namePart
    }
    Interval(name,time,time,data)
  }

  def readFile(name: String): List[Interval] = {
    val writer = new BufferedWriter(new FileWriter(new File("results/evrs.txt")))
    var intervals : List[Interval] = Nil
    for (line <- Source.fromFile(name).getLines) {
      val interval = mkInterval(line)
      intervals ++= List(interval)
      // Interval(name: String, begin: Sclk, end: Sclk, data: Map[String, Any])
      val name = interval.name + ","
      val time = interval.begin.toInt
      val cmd = if (interval.data.isDefinedAt("value")) interval.data("value") + "," else ""
      val dejauLine = s"$name$cmd$time\n"
      print(dejauLine)
      writer.write(dejauLine)
    }
    writer.close()
    return intervals
  }
}

object Test16 extends UnitTest {
  def main(args: Array[String]): Unit = {
    // implicit val nfer = new Nfer().dotDir("results/dotfiles").delay(true)
    implicit val nfer = new Nfer()

    val specText =
      """
      boot :- boot_s before boot_e
        where boot_s.value = boot_e.value
        map {value -> boot_s.value}

      dboot :- b1:boot before b2:boot
        where (b2.end - b1.begin) < 300

      risk :- downlink during dboot

      """

    nfer.specText(specText)

    val intervals: List[Interval] =
      new FileReader().readFile("src/test/resources/prolog/prolog.events.txt")
    var count : Int = 0
    println("start")
    nfer.time[Unit] {
      for (interval <- intervals) {
        count += 1
        nfer.submit(interval)
      }
    }
    println(s"$count intervals processed")

    //verify("boot")()
    //verify("dboot")()
    //verify("risk")()
  }
}



