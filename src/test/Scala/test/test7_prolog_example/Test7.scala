package test.test7_prolog_example

import nfer._
import test._
import scala.io.Source



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
    var intervals : List[Interval] = Nil
    for (line <- Source.fromFile(name).getLines) {
      intervals ++= List(mkInterval(line))
    }
    return intervals
  }
}

object Test7 extends UnitTest {
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
    verify("risk")()
  }
}



