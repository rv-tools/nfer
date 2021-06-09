package test.test10_msl_casestudy

import nfer._
import test._


object Test10 extends UnitTest {
  def main(args: Array[String]): Unit = {
    implicit val nfer = new Nfer().dotDir("results/dotfiles").delay(false)
    // implicit val nfer = new Nfer()

    nfer.PRINT_SUBMIT = true

    val spec1 =
      """
      cmdExec :- CMD_DISPATCH before CMD_COMPLETE
        where CMD_DISPATCH.cmd = CMD_COMPLETE.cmd
        map {cmd -> CMD_DISPATCH.cmd}

       okRace :- TLM_TR_ERROR during cmdExec
         where cmdExec.cmd = "MOB_PRM" | cmdExec.cmd = "ARM_PRM"
      """

    val spec2 =
      """
        okCmdFail  :-  INST_PWR_ON  before INST_CMD_FAIL  before INST_RECOVER
          where this.end - this.begin <= 15
      """

    val spec3 =
      """
        comm :- COMM_BEGIN before COMM_END
          map {id -> COMM_BEGIN.id}

        vdp :- VDP_START before VDP_STOP

        okStarvation :- TASK_STARVATION during (vdp slice comm)
          map {id -> comm.id}
      """

    def test1() {
      nfer.specText(spec1)
      nfer.submit("CMD_DISPATCH", 10, "cmd" -> "CMD")
      nfer.submit("TLM_TR_ERROR", 20)
      nfer.submit("CMD_COMPLETE", 30, "cmd" -> "CMD")
      nfer.submit("CMD_DISPATCH", 40, "cmd" -> "ARM_PRM")
      nfer.submit("TLM_TR_ERROR", 50)
      nfer.submit("CMD_COMPLETE", 60, "cmd" -> "ARM_PRM")
      verify("okRace")(
        P(
          Interval("okRace", 40.0, 60.0, Map())),
        P(
          Interval("TLM_TR_ERROR", 20.0, 20.0, Map()),
          Interval("TLM_TR_ERROR", 50.0, 50.0, Map())),
        P(
          Interval("cmdExec", 10.0, 30.0, Map("cmd" -> "CMD")),
          Interval("cmdExec", 40.0, 60.0, Map("cmd" -> "ARM_PRM"))))
    }

    def test2(): Unit = {
      nfer.specText(spec2)
      nfer.submit("INST_PWR_ON", 10)
      nfer.submit("INST_CMD_FAIL", 20)
      nfer.submit("INST_RECOVER", 30)
      nfer.submit("INST_PWR_ON", 40)
      nfer.submit("INST_CMD_FAIL", 50)
      nfer.submit("INST_RECOVER", 55)
      verify("okCmdFail")(
        P(
          Interval("okCmdFail",40.0,55.0,Map())),
        P(
          Interval("N_2",10.0,20.0,Map()),
          Interval("N_2",40.0,50.0,Map())),
        P(
          Interval("INST_PWR_ON",10.0,10.0,Map()),
          Interval("INST_PWR_ON",40.0,40.0,Map())),
        P(
          Interval("INST_CMD_FAIL",20.0,20.0,Map()),
          Interval("INST_CMD_FAIL",50.0,50.0,Map())),
        P(
          Interval("INST_RECOVER",30.0,30.0,Map()),
          Interval("INST_RECOVER",55.0,55.0,Map())))
    }

    def test3(): Unit = {
      nfer.specText(spec3)
      nfer.submit("COMM_BEGIN", 100, "id" -> 42)
      nfer.submit("VDP_START", 150)
      nfer.submit("TASK_STARVATION", 175)
      nfer.submit("COMM_END", 200)
      nfer.submit("VDP_STOP", 250)
      verify("okStarvation")()
    }

    test3()
  }
}
