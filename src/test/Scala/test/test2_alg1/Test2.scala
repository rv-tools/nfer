package test.test2_alg1

import nfer._

object Test2 {
  def main(args: Array[String]): Unit = {
    val nfer = new Nfer().dotDir("results/dotfiles").delay(false)

    val specText =
      """

         BOOT_SS :- BOOT_S where BOOT_S.count > 42 map {count -> BOOT_S.count}

         BOOT :- BOOT_SS before BOOT_E where BOOT_E.begin - BOOT_SS.end <= 5000 map {count -> BOOT_SS.count}

         DBOOT :- b1:BOOT before b2:BOOT where b2.begin - b1.end <= 10000 map {count -> b1.count}

         RISK :- DOWNLINK during DBOOT map {count -> DBOOT.count}

      """


    nfer.specText(specText)

    nfer.submit("BOOT_S", 1000, "count" -> 42)
    nfer.submit("BOOT_E", 2000)
    nfer.submit("BOOT_S", 3000, "count" -> 43)
    nfer.submit("DOWNLINK", 4000)
    nfer.submit("BOOT_E", 5000)
  }
}


