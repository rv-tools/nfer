package nfer.ast

import scala.util.parsing.combinator.RegexParsers

/**
 * Created by seanmk on 11/30/16.
 */

private[nfer] object NferTokenNamespace {
  sealed trait NferToken

  case class IDENTIFIER(name: String) extends NferToken
  case class INTLITERAL(i: Int) extends NferToken
  case class DOUBLELITERAL(f: Double) extends NferToken
  case class STRINGLITERAL(s: String) extends NferToken
  case object SPEC extends NferToken
  case object MODULE extends NferToken
  case object IMPORT extends NferToken
  case object WHEN extends NferToken
  case object BANG extends NferToken
  case object PLUS extends NferToken
  case object MINUS extends NferToken
  case object MUL extends NferToken
  case object DIV extends NferToken
  case object MOD extends NferToken
  case object EQ extends NferToken
  case object NE extends NferToken
  case object LT extends NferToken
  case object LE extends NferToken
  case object GT extends NferToken
  case object GE extends NferToken
  case object AND extends NferToken
  case object OR extends NferToken
  case object CALL extends NferToken
  case object ALSO extends NferToken
  case object BEFORE extends NferToken
  case object DURING extends NferToken
  case object START extends NferToken
  case object FINISH extends NferToken
  case object MEET extends NferToken
  case object OVERLAP extends NferToken
  case object SLICE extends NferToken
  case object COINCIDE extends NferToken
  case object LPAREN extends NferToken
  case object RPAREN extends NferToken
  case object LBRACE extends NferToken
  case object RBRACE extends NferToken
  case object DOT extends NferToken
  case object COMMA extends NferToken
  case object SEMICOLON extends NferToken
  case object MAPSTO extends NferToken
  case object COLON extends NferToken
  case object MAP extends NferToken
  case object WHERE extends NferToken
  case object BEGIN extends NferToken
  case object END extends NferToken
  case object TRUE extends NferToken
  case object FALSE extends NferToken
}

import NferTokenNamespace._

private[nfer] object NferLexer extends RegexParsers {
  override def skipWhitespace = true
  protected override val whiteSpace = """(\s|/\*(.|\n|\r)*?\*/|//.*\n)+""".r

  val floatingPoint = """(\d+\.\d*|\d*\.\d+)([eE][+-]?\d+)?[fFdD]?""".r
  val wholeNumber = """(\d+)""".r
  val string = """"[^"]*"""".r

  def identifier: Parser[IDENTIFIER] =
    "[a-zA-Z_][a-zA-Z0-9_]*".r ^^ { i => IDENTIFIER(i) }

  def intLiteral: Parser[INTLITERAL] =
    wholeNumber ^^ { w => INTLITERAL(w.toInt) }

  def doubleLiteral: Parser[DOUBLELITERAL] =
    floatingPoint ^^ { f => DOUBLELITERAL(f.toDouble) }

  def stringLiteral: Parser[STRINGLITERAL] =
    string ^^ { s => STRINGLITERAL(s.stripPrefix("\"").stripSuffix("\""))}

  def spec      = "spec"     ^^ (_ => SPEC)
  def module    = "module"   ^^ (_ => MODULE)
  def import_   = "import"   ^^ (_ => IMPORT)
  def when      = ":-"       ^^ (_ => WHEN)
  def bang      = "!"        ^^ (_ => BANG)
  def plus      = "+"        ^^ (_ => PLUS)
  def minus     = "-"        ^^ (_ => MINUS)
  def mul       = "*"        ^^ (_ => MUL)
  def div       = "/"        ^^ (_ => DIV)
  def mod       = "%"        ^^ (_ => MOD)
  def eq        = "="        ^^ (_ => EQ)
  def ne        = "!="       ^^ (_ => NE)
  def le        = "<="       ^^ (_ => LE)
  def lt        = "<"        ^^ (_ => LT)
  def ge        = ">="       ^^ (_ => GE)
  def gt        = ">"        ^^ (_ => GT)
  def and       = "&"        ^^ (_ => AND)
  def or        = "|"        ^^ (_ => OR)
  def call      = "call"     ^^ (_ => CALL)
  def also      = "also"     ^^ (_ => ALSO)
  def before    = "before"   ^^ (_ => BEFORE)
  def during    = "during"   ^^ (_ => DURING)
  def start     = "start"    ^^ (_ => START)
  def finish    = "finish"   ^^ (_ => FINISH)
  def meet      = "meet"     ^^ (_ => MEET)
  def overlap   = "overlap"  ^^ (_ => OVERLAP)
  def slice     = "slice"    ^^ (_ => SLICE)
  def coincide  = "coincide" ^^ (_ => COINCIDE)
  def lparen    = "("        ^^ (_ => LPAREN)
  def rparen    = ")"        ^^ (_ => RPAREN)
  def lbrace    = "{"        ^^ (_ => LBRACE)
  def rbrace    = "}"        ^^ (_ => RBRACE)
  def dot       = "."        ^^ (_ => DOT)
  def comma     = ","        ^^ (_ => COMMA)
  def semicolon = ";"        ^^ (_ => SEMICOLON)
  def mapsto    = "->"       ^^ (_ => MAPSTO)
  def colon     = ":"        ^^ (_ => COLON)
  def map       = "map"      ^^ (_ => MAP)
  def where     = "where"    ^^ (_ => WHERE)
  def begin     = "begin"    ^^ (_ => BEGIN)
  def end       = "end"      ^^ (_ => END)
  def true_     = "true"     ^^ (_ => TRUE)
  def false_    = "false"    ^^ (_ => FALSE)

  def tokens: Parser[List[NferToken]] = {
    phrase(rep1(spec | module | import_ | mapsto | lparen | rparen | lbrace | rbrace | dot | comma | semicolon |
      ne | eq | le | lt | ge | gt | and | or | mod | div | mul | minus | plus | bang | call | when | colon |
      also | before | during | start | finish | meet | overlap | slice | coincide | true_ | false_ |
      map | where | begin | end | doubleLiteral | intLiteral | stringLiteral | identifier ))
  }

  def apply(code: String): List[NferToken] = {
    parseAll(tokens, code) match {
      case fail@NoSuccess(_, _) => {
        println(fail)
        List()
      }
      case Success(result, next) => result
    }
  }
}

private[nfer] object TestLexer {
  def main(args: Array[String]): Unit = {

    val test1 = "A :- B before c:C where c.end - B.begin < 300.21 map { foo -> c.foo }"
    println("------------------")
    println(test1)
    println("------------------")
    NferLexer(test1) foreach println

    println()

    val test2 = """A :- B with C where B.end = C.begin & B.foo = "bar" """
    println("------------------")
    println(test2)
    println("------------------")
    NferLexer(test2) foreach println
  }
}