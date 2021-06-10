package test.test0

import nfer._

object Example {
  def main(args: Array[String]): Unit = {
    val nfer = new Nfer()

    nfer.specFile("src/test/Scala/test/test0/spec.nfer")

    nfer.submit("BOOT_S", 12923, "count" -> 42)
    nfer.submit("BOOT_E", 13112)
    nfer.submit("DOWNLINK", 20439)
    nfer.submit("BOOT_S", 27626, "count" -> 43)
    nfer.submit("BOOT_E", 48028)

    nfer.printIntervals()
  }
}

