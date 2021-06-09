package nfer.ast

private[nfer] object AstTypes {
  type Name = String
}

import AstTypes._

private[nfer] case class Specification(modules: List[Module]) {
  override def toString = {
    var result = ""
    for (module <- modules) result += module
    result
  }

  def getRules: List[Rule] = {
    def getModule(name: String): Module =
      (modules filter (m => m.name == name)).head

    def extractModuleNames(name: String): Set[String] = {
      val module = getModule(name)
      (for (imp <- module.imports) yield extractModuleNames(imp)).toSet.flatten + name
    }

    val moduleNames = extractModuleNames(modules.last.name)

    (for (module <- modules if moduleNames contains module.name) yield module.rules).flatten
  }

}

private[nfer] case class Module(name: Name, imports: List[Name], rules: List[Rule]) {
  override def toString = {
    var result = s"  module $name {\n"
    if (imports != Nil) result += s"    import ${imports.mkString(",")}\n\n"
    for (rule <- rules) {
      result += s"    $rule"
    }
    result += "  }"
    result
  }
}

private[nfer] case class Rule(
                 name: Name,
                 body: IntervalExpression,
                 constraint: Option[Expression],
                 mapping: Option[Map[String, Expression]],
                 endPoints: Option[(Expression, Expression)]) {
  override def toString = {
    var result = s"$name :- $body\n"
     constraint match {
       case None =>
       case Some(exp) =>
         result += s"      where $exp\n"
     }
     mapping match {
       case None =>
       case Some(map) =>
         result += s"      map {${map.mkString(",")}}\n"
      }
    endPoints match {
      case None =>
      case Some((exp1,exp2)) =>
        result += s"      begin $exp1 end $exp2\n"
    }
    result
  }
}

private[nfer] sealed trait IntervalExpression

private[nfer] case class AtomicIntervalExpression(name: Name, label : Option[Name] = None) extends IntervalExpression {
  override def toString = {
    (label match {
      case None => ""
      case Some(labelName) => s"$labelName:"
    }) + name
  }
}

private[nfer] case class BinaryIntervalExpression(
                                     exp1: IntervalExpression,
                                     operand: IntervalOperand,
                                     exp2: IntervalExpression
                                   ) extends IntervalExpression {
  override def toString =  s"$exp1 $operand $exp2"
}

private[nfer] case class ParenIntervalExpression(exp: IntervalExpression) extends IntervalExpression {
  override def toString = s"($exp)"
}

private[nfer] sealed trait Expression

private[nfer] case class BooleanLiteral(b: Boolean) extends Expression {
  override def toString = b.toString
}

private[nfer] case class IntLiteral(i: Int) extends Expression {
  override def toString = i.toString
}

private[nfer] case class StringLiteral(s: String) extends Expression {
  override def toString = s""""$s""""
}

private[nfer] case class DoubleLiteral(d: Double) extends Expression {
  override def toString = d.toString
}

private[nfer] case class BeginTime(source: String) extends Expression {
  override def toString = s"$source.begin"
}

private[nfer] case class EndTime(source: String) extends Expression {
  override def toString = s"$source.end"
}

private[nfer] case class MapField(source: String, id: String) extends Expression {
  override def toString = s"$source.$id"
}

private[nfer] case class UnaryExpression(operand: UnaryOperand, exp: Expression) extends Expression {
  override def toString = s"$operand($exp)"
}

private[nfer] case class BinaryExpression(exp1: Expression, operand: BinaryOperand, exp2: Expression) extends Expression {
  override def toString = s"$exp1 $operand $exp2"
}

private[nfer] case class ParenExpression(exp: Expression) extends Expression {
  override def toString = s"($exp)"
}

private[nfer] case class CallExpression(name: String, exps: List[Expression]) extends Expression {
  override def toString = s"$name(${exps.mkString(",")})"
}

private[nfer] sealed trait UnaryOperand

private[nfer] case object Neg extends UnaryOperand {
  override def toString = "-"
}

private[nfer] case object Not extends UnaryOperand {
  override def toString = "!"
}

private[nfer] sealed trait BinaryOperand

private[nfer] case object Add extends BinaryOperand {
  override def toString = "+"
}

private[nfer] case object Sub extends BinaryOperand {
  override def toString = "-"
}

private[nfer] case object Mul extends BinaryOperand {
  override def toString = "*"
}

private[nfer] case object Div extends BinaryOperand {
  override def toString = "/"
}

private[nfer] case object Mod extends BinaryOperand {
  override def toString = "%"
}

private[nfer] case object Lt extends BinaryOperand {
  override def toString = "<"
}

private[nfer] case object Le extends BinaryOperand {
  override def toString = "<="
}

private[nfer] case object Gt extends BinaryOperand {
  override def toString = ">"
}

private[nfer] case object Ge extends BinaryOperand {
  override def toString = ">="
}

private[nfer] case object Eq extends BinaryOperand {
  override def toString = "="
}

private[nfer] case object Ne extends BinaryOperand {
  override def toString = "!="
}

private[nfer] case object And extends BinaryOperand {
  override def toString = "&"
}

private[nfer] case object Or extends BinaryOperand {
  override def toString = "|"
}

private[nfer] sealed trait IntervalOperand

private[nfer] case object Also extends IntervalOperand {
  override def toString = "also"
}

private[nfer] case object Before extends IntervalOperand {
  override def toString = "before"
}

private[nfer] case object During extends IntervalOperand {
  override def toString = "during"
}

private[nfer] case object Start extends IntervalOperand {
  override def toString = "start"
}

private[nfer] case object Finish extends IntervalOperand {
  override def toString = "finish"
}

private[nfer] case object Meet extends IntervalOperand {
  override def toString = "meet"
}

private[nfer] case object Overlap extends IntervalOperand {
  override def toString = "overlap"
}

private[nfer] case object Slice extends IntervalOperand {
  override def toString = "slice"
}

private[nfer] case object Coincide extends IntervalOperand {
  override def toString = "coincide"
}


private[nfer] object MainAST {
  def main(args: Array[String]): Unit = {
    println("""

        |         __
        |        / _|
        |  _ __ | |_ ___ _ __
        | | '_ \|  _/ _ \ '__|
        | | | | | ||  __/ |
        | |_| |_|_| \___|_|
        |
        |
        |
      """.stripMargin)
  }
}