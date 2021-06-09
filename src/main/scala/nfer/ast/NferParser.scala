package nfer.ast

import scala.util.parsing.combinator._
import scala.util.parsing.input.{Reader, Position, NoPosition}
import scala.language.postfixOps
import NferTokenNamespace._

/**
  * Created by seanmk on 11/28/16.
  * Continued edits by klaus Havelund
  */

private[nfer] class NferTokenReader(tokens: Seq[NferToken]) extends Reader[NferToken] {
  override def first: NferToken = tokens.head

  override def atEnd: Boolean = tokens.isEmpty

  override def pos: Position = NoPosition

  override def rest: Reader[NferToken] = new NferTokenReader(tokens.tail)
}

import AstTypes._

private[nfer] object NferParser extends Parsers {
  override type Elem = NferToken

  def specification: Parser[Specification] =
    rep1(rule) ^^ {
      case rules => Specification(Module("Rules", Nil, rules) :: Nil)
    } |
      rep1(module) ^^ {
        case mds => Specification(mds)
      }

  def module: Parser[Module] =
    MODULE ~ identifier ~ LBRACE ~ opt(imports) ~ rep(rule) ~ RBRACE ^^ {
      case _ ~ id ~ _ ~ optimps ~ rules ~ _ =>
        val imports = optimps match {
          case None => Nil
          case Some(strs) => strs
        }
        Module(id.name, imports, rules)
    }

  def imports: Parser[List[String]] =
    IMPORT ~ rep1sep(identifier, COMMA) ~ SEMICOLON ^^ {
      case _ ~ ids ~ _ =>
        ids.map(_.name)
    }

  def rule: Parser[Rule] =
    identifier ~ WHEN ~ intervalExpression ~ opt(whereExpression) ~ opt(mapExpression) ~ opt(endPoints) ^^ {
      case id ~ _ ~ e ~ wh ~ mp ~ ep =>
        Rule(id.name, e, wh, mp, ep)
    }

  def intervalExpression: Parser[IntervalExpression] =
    primaryIntervalExpression ~ rep(intervalOp ~ primaryIntervalExpression) ^^ {
      case u ~ l => l.foldLeft(u)((left, right) => BinaryIntervalExpression(left, right._1, right._2))
    }

  def primaryIntervalExpression: Parser[IntervalExpression] =
    atomicIntervalExpression |
      parenIntervalExpression

  def atomicIntervalExpression: Parser[IntervalExpression] = {
    opt(label) ~ identifier ^^ {
      case lab ~ id => AtomicIntervalExpression(id.name, lab)
    }
  }

  def parenIntervalExpression: Parser[IntervalExpression] =
    LPAREN ~ intervalExpression ~ RPAREN ^^ {
      case _ ~ exp ~ _ => ParenIntervalExpression(exp)
    }

  def label: Parser[String] =
    identifier <~ COLON ^^ { case id => id.name }

  def whereExpression: Parser[Expression] =
    WHERE ~> expression

  def mapExpression: Parser[Map[String, Expression]] =
    MAP ~ LBRACE ~ rep1sep(identifier ~ MAPSTO ~ expression, COMMA) ~ RBRACE ^^ {
      case _ ~ _ ~ m ~ _ =>
        var map: Map[String, Expression] = Map()
        for ((id ~ _ ~ exp) <- m)
          map += (id.name -> exp)
        map
    }

  def endPoints: Parser[(Expression, Expression)] =
    BEGIN ~ expression ~ END ~ expression ^^ {
      case _ ~ exp1 ~ _ ~ exp2 => (exp1, exp2)
    }

  def intervalOp: Parser[IntervalOperand] =
    ALSO ^^ (_ => Also) |
      BEFORE ^^ (_ => Before) |
      MEET ^^ (_ => Meet) |
      DURING ^^ (_ => During) |
      START ^^ (_ => Start) |
      FINISH ^^ (_ => Finish) |
      OVERLAP ^^ (_ => Overlap) |
      SLICE ^^ (_ => Slice) |
      COINCIDE ^^ (_ => Coincide)

  def expression: Parser[Expression] =
    comparisonExpression ~ rep(andorOp ~ comparisonExpression) ^^ {
      case u ~ l => l.foldLeft(u)((left, right) => BinaryExpression(left, right._1, right._2))
    }

  def comparisonExpression: Parser[Expression] =
    plusminusExpression ~ rep(comparisonOp ~ plusminusExpression) ^^ {
      case u ~ l => l.foldLeft(u)((left, right) => BinaryExpression(left, right._1, right._2))
    }

  def plusminusExpression: Parser[Expression] =
    muldivExpression ~ rep(plusminusOp ~ muldivExpression) ^^ {
      case u ~ l => l.foldLeft(u)((left, right) => BinaryExpression(left, right._1, right._2))
    }

  def muldivExpression: Parser[Expression] =
    unaryExpression ~ rep(muldivOp ~ unaryExpression) ^^ {
      case u ~ l => l.foldLeft(u)((left, right) => BinaryExpression(left, right._1, right._2))
    }

  def unaryExpression: Parser[Expression] = {
    stringLiteral ^^ {
      case s => StringLiteral(s.s)
    } |
      intLiteral ^^ {
        case i => IntLiteral(i.i)
      } |
      doubleLiteral ^^ {
        case d => DoubleLiteral(d.f)
      } |
      booleanLiteral ^^ {
        case b => BooleanLiteral(b)
      } |
      beginTime |
      endTime |
      mapField |
      callExpression |
      LPAREN ~ expression ~ RPAREN ^^ {
        case _ ~ exp ~ _ =>
          ParenExpression(exp)
      } |
      unaryOp ~ unaryExpression ^^ {
        case op ~ exp => UnaryExpression(op, exp)
      }
  }

  def mapField: Parser[MapField] =
    identifier ~ DOT ~ identifier ^^ { case src ~ _ ~ id => MapField(src.name, id.name) }

  def beginTime: Parser[BeginTime] =
    identifier ~ DOT ~ BEGIN ^^ { case id ~ _ ~ _ => BeginTime(id.name) }

  def endTime: Parser[EndTime] =
    identifier ~ DOT ~ END ^^ { case id ~ _ ~ _ => EndTime(id.name) }

  def unaryOp: Parser[UnaryOperand] =
    MINUS ^^ (_ => Neg) |
      BANG ^^ (_ => Not)

  def muldivOp: Parser[BinaryOperand] =
    MUL ^^ (_ => Mul) | DIV ^^ (_ => Div) | MOD ^^ (_ => Mod)

  def plusminusOp: Parser[BinaryOperand] =
    PLUS ^^ (_ => Add) | MINUS ^^ (_ => Sub)

  def comparisonOp: Parser[BinaryOperand] =
    LT ^^ (_ => Lt) |
      LE ^^ (_ => Le) |
      GT ^^ (_ => Gt) |
      GE ^^ (_ => Ge) |
      EQ ^^ (_ => Eq) |
      NE ^^ (_ => Ne)

  def andorOp: Parser[BinaryOperand] =
    AND ^^ (_ => And) | OR ^^ (_ => Or)

  private def identifier: Parser[IDENTIFIER] =
    accept("identifier", { case id@IDENTIFIER(name) => id })

  private def stringLiteral: Parser[STRINGLITERAL] =
    accept("string literal", { case lit@STRINGLITERAL(value) => lit })

  private def intLiteral: Parser[INTLITERAL] =
    accept("int literal", { case lit@INTLITERAL(value) => lit })

  private def doubleLiteral: Parser[DOUBLELITERAL] =
    accept("double literal", { case lit@DOUBLELITERAL(value) => lit })

  def booleanLiteral: Parser[Boolean] =
    TRUE ^^ (_ => true) | FALSE ^^ (_ => false)

  def callExpression: Parser[Expression] =
    identifier ~ LPAREN ~ repsep(expression, COMMA) ~ RPAREN ^^ {
      case id ~ _ ~ exps ~ _ => CallExpression(id.name,exps)
    }

  def apply(tokens: Seq[NferToken]): Specification = {
    val reader = new NferTokenReader(tokens)
    specification(reader) match {
      case fail@NoSuccess(_, _) => {
        println(fail)
        assert(false).asInstanceOf[Specification]
      }
      case Success(result, next) =>
        assert(next.atEnd, s"\n\n-----\nnot all input has been processed:\nProcessed symbols: $result\nNext unprocessed symbol: ${next.first}\n-----\n")
        result
    }
  }

  def apply(input: String): Specification = {
    NferParser.apply(NferLexer(input))
  }
}

private[nfer] object TestParser {
  def main(args: Array[String]): Unit = {

    val rule1 = "A :- B before ((C meet D) during E) where foo.bar < 300 map { foo -> c.foo }"
    val tokens1 = NferLexer(rule1)
    println(rule1)
    println(tokens1)
    println(NferParser(tokens1))

    println("-----------------")

    val rule2 = "A :- B before c:C where c.end - B.begin < 300.21 | 1 + 3 * 4 - 3 > 0 map { foo -> c.foo }"
    val tokens2 = NferLexer(rule2)
    println(rule2)
    println(tokens2)
    println(NferParser(tokens2))

    println("-----------------")

    val rule3 = """A :- B also C where B.end = C.begin & B.foo = "bar" """
    val tokens3 = NferLexer(rule3)
    println(rule3)
    println(tokens3)
    println(NferParser(tokens3))

    println("-----------------")

    val rule4 = "A :- A during (B before C) where C.begin - B.end  > 5 map { P -> A.x + 1 } begin B.begin - 1 end this.end"
    println(rule4)
    val rule4Ast = NferParser(rule4)
    println(rule4Ast)
  }
}