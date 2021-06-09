package nfer.repr

import nfer.repr.nodes.node4.Node
import Math._
import java.io._

import nfer.ast._
import nfer.{Interval, Operation, Sclk}

private[nfer] object Util {
  type IntervalName = String
  type Env = Map[IntervalName, Interval]
  type Data = Map[String, Any]

  private var fileName: String = "test.dot"
  val file: BufferedWriter = new BufferedWriter(new FileWriter(new File(fileName)))

  /**
    * Option. True when minimality is applied. Default value is true.
    */

  var MINIMALITY : Boolean = true

  def printDot(str: String): Unit = {
    file.write(str)
    file.flush()
  }

  def NOT_DONE: Nothing = {
    assert(false, "NOT IMPLEMENTED").asInstanceOf[Nothing]
  }

  def debug(msg: String): Unit = {
    println(s"---> $msg")
  }
}

import Util._

// -------
// Tokens:
// -------

/**
  * The environment is a 'var' since we need to nullify it in algorithm 2.
  */

private[nfer] case class Token(interval: Interval, var environment: Map[IntervalName, Interval]) {
  override def toString: String = {
    val env = environment.map(_._1).mkString(",")
    // s"$interval@$env"
    s"${interval.toStringCompressed}"
  }
}

// ----------------------
// Expression evaluation:
// ----------------------

private[nfer] class OperationStore {
  private var operationMap: Map[String, Operation] = Map()

  def add(name: String)(operation: Operation): Unit = {
    operationMap += (name -> operation)
  }

  def apply(name: String)(args: Any*): Any =
    operationMap(name)(args: _*)
}

private[nfer] object ExpressionEvaluation {
  var operationStore = new OperationStore

  def convert(value: Any): Double =
    if (value.isInstanceOf[Int])
      value.asInstanceOf[Int].toDouble
    else value.asInstanceOf[Double]

  def evaluate(exp: Expression)(token: Token): Any = {
    val Token(Interval(_, begin, end, _), env) = token
    exp match {
      case BooleanLiteral(bool) =>
        bool
      case IntLiteral(int) =>
        int
      case StringLiteral(string) =>
        string
      case DoubleLiteral(double) =>
        double
      case BeginTime(source) =>
        if (source == "this")
          begin
        else
          env(source).begin
      case EndTime(source) =>
        if (source == "this")
          end
        else
          env(source).end
      case MapField(source, id) =>
        env(source).data(id)
      case UnaryExpression(operand, exp) =>
        val value = evaluate(exp)(token)
        operand match {
          case Neg =>
            - convert(value)
          case Not =>
            !(value.asInstanceOf[Boolean])
        }
      case BinaryExpression(exp1, operand, exp2) =>
        val value1 = evaluate(exp1)(token)
        val value2 = evaluate(exp2)(token)
        operand match {
          case Add =>
            convert(value1) + convert(value2)
          case Sub =>
            convert(value1) - convert(value2)
          case Mul =>
            convert(value1) * convert(value2)
          case Div =>
            convert(value1) / convert(value2)
          case Mod =>
            convert(value1) % convert(value2)
          case Lt =>
            convert(value1) < convert(value2)
          case Le =>
            convert(value1) <= convert(value2)
          case Gt =>
            convert(value1) > convert(value2)
          case Ge =>
            convert(value1) >= convert(value2)
          case Eq =>
            value1 == value2
          case Ne =>
            value1 != value2
          case And =>
            value1.asInstanceOf[Boolean] && value2.asInstanceOf[Boolean]
          case Or =>
            value1.asInstanceOf[Boolean] || value2.asInstanceOf[Boolean]
        }
      case CallExpression(name, exps) =>
        val arguments = exps map (evaluate(_)(token))
        operationStore(name)(arguments: _*)
      case ParenExpression(exp) =>
        evaluate(exp)(token)
    }
  }
}


private[nfer] object Node {
  private var nodeCounter: Int = 0
  private var nameCounter: Int = 0

  def nextNumber: Int = {
    nodeCounter += 1
    nodeCounter
  }

  def nextName: String = {
    nameCounter += 1
    StringContext("N_", "").s(nameCounter)
  }
}

// ------------------
// Specialized Nodes:
// ------------------

private[nfer] class AtomicNode extends Node

private[nfer] class OneChildTopNode extends Node

private[nfer] class AlsoNode extends Node

private[nfer] class BeforeNode extends Node {
  override def predefinedOrdered(interval1: Interval, interval2: Interval): Boolean = {
    if (interval1.isAtomic && interval2.isAtomic)
      interval1.end < interval2.begin
    else
      interval1.end <= interval2.begin
  }

  override def predefinedEndPoints(interval1: Interval, interval2: Interval): Tuple2[Sclk, Sclk] = {
    (interval1.begin, interval2.end)
  }
}

private[nfer] class DuringNode extends Node {
  override def predefinedOrdered(interval1: Interval, interval2: Interval): Boolean = {
    interval1.begin >= interval2.begin &&
      interval1.end <= interval2.end
  }

  override def predefinedEndPoints(interval1: Interval, interval2: Interval): Tuple2[Sclk, Sclk] = {
    (interval2.begin, interval2.end)
  }
}

private[nfer] class StartNode extends Node {
  override def predefinedOrdered(interval1: Interval, interval2: Interval): Boolean = {
    interval1.begin == interval2.begin
  }

  override def predefinedEndPoints(interval1: Interval, interval2: Interval): Tuple2[Sclk, Sclk] = {
    (interval1.begin, max(interval1.end, interval2.end))
  }
}

private[nfer] class FinishNode extends Node {
  override def predefinedOrdered(interval1: Interval, interval2: Interval): Boolean = {
    interval1.end == interval2.end
  }

  override def predefinedEndPoints(interval1: Interval, interval2: Interval): Tuple2[Sclk, Sclk] = {
    (min(interval1.begin, interval2.begin), interval1.end)
  }
}

private[nfer] class MeetNode extends Node {
  override def predefinedOrdered(interval1: Interval, interval2: Interval): Boolean = {
    interval1.end == interval2.begin
  }

  override def predefinedEndPoints(interval1: Interval, interval2: Interval): Tuple2[Sclk, Sclk] = {
    (interval1.begin, interval2.end)
  }
}

private[nfer] class OverlapNode extends Node {
  override def predefinedOrdered(interval1: Interval, interval2: Interval): Boolean = {
    interval1.begin < interval2.end && interval2.begin < interval1.end
  }

  override def predefinedEndPoints(interval1: Interval, interval2: Interval): Tuple2[Sclk, Sclk] = {
    (min(interval1.begin, interval2.begin), max(interval1.end, interval2.end))
  }
}

private[nfer] class SliceNode extends Node {
  override def predefinedOrdered(interval1: Interval, interval2: Interval): Boolean = {
    interval1.begin < interval2.end && interval2.begin < interval1.end
  }

  override def predefinedEndPoints(interval1: Interval, interval2: Interval): Tuple2[Sclk, Sclk] = {
    (max(interval1.begin, interval2.begin), min(interval1.end, interval2.end))
  }
}

private[nfer] class CoincideNode extends Node {
  override def predefinedOrdered(interval1: Interval, interval2: Interval): Boolean = {
    interval1.begin == interval2.begin &&
      interval1.end == interval2.end
  }

  override def predefinedEndPoints(interval1: Interval, interval2: Interval): Tuple2[Sclk, Sclk] = {
    (interval1.begin, interval1.end)
  }
}

// --------------------
// Rule Representation:
// --------------------

private[nfer] class RuleRepr {
  private var map: Map[IntervalName, Set[Node]] = Map()
  private var topNode: Node = null
  private var dotFileName: String = null

  def setDotFile(name: String): Unit = {
    dotFileName = name
    printDot()
  }

  def setCallBack(cb: Interval => Unit): Unit = {
    getTopNode.setCallBack(cb)
  }

  def printDot(): Unit = {
    if (dotFileName != null) {
      val file: BufferedWriter = new BufferedWriter(new FileWriter(new File(dotFileName)))
      file.write(getTopNode.toString)
      file.close()
    }
  }

  def addEntry(name: IntervalName, node: Node): Unit = {
    val nodeSet = map.getOrElse(name, Set())
    map += (name -> (nodeSet + node))
  }

  def getNodes(name: IntervalName): Set[Node] =
    map(name)

  def setTopNode(node: Node): Unit = {
    topNode = node
  }

  def getTopNode: Node = topNode

  def getIntervalNames: Set[String] =
    map.keySet

  def getIntervals: List[Set[Interval]] =
    topNode.getIntervals

  def addInterval(interval: Interval): Unit = {
    for (node <- getNodes(interval.name)) {
      node.addInterval(interval)
    }
    printDot()
  }

  def addEvent(name: IntervalName, time: Sclk, data: (String, Any)*) {
    addInterval(Interval(name, time, time, data.toMap))
  }

  override def toString: String = {
    var result = "NodeMap{\n"
    for ((name, nodeSet) <- map) {
      result += s"  $name -> {${nodeSet.map(_.number).mkString(",")}\n"
    }
    result += "}"
    result
  }
}

// ---------------------------
// Translating AST to network:
// ---------------------------

private[nfer] object RuleAstToRuleRepr {
  def apply(rule: Rule): RuleRepr =
    convertRule(rule)

  def convertRule(rule: Rule): RuleRepr = {
    val nodeMap = new RuleRepr
    val node = convertIntervalExpression(rule.body)(nodeMap)
    node.name = rule.name
    node.rule = rule
    nodeMap.setTopNode(node)
    nodeMap
  }

  def convertIntervalExpression(exp: IntervalExpression)(nodeMap: RuleRepr, parent: Node = null): Node = {
    exp match {
      case AtomicIntervalExpression(name, label) =>
        val node = new AtomicNode
        node.name = label match {
          case None => name
          case Some(lab) => lab
        }
        nodeMap.addEntry(name, node)
        if (parent != null) {
          node.outputNode = parent
          node
        } else {
          val topNode = new OneChildTopNode
          topNode.inputNode2 = node
          node.outputNode = topNode
          topNode
        }
      case BinaryIntervalExpression(exp1, operand, exp2) =>
        val node = operand match {
          case Also => new AlsoNode
          case Before => new BeforeNode
          case During => new DuringNode
          case Start => new StartNode
          case Finish => new FinishNode
          case Meet => new MeetNode
          case Overlap => new OverlapNode
          case Slice => new SliceNode
          case Coincide => new CoincideNode
        }
        node.inputNode1 = convertIntervalExpression(exp1)(nodeMap, node)
        node.inputNode2 = convertIntervalExpression(exp2)(nodeMap, node)
        node.outputNode = parent
        node
      case ParenIntervalExpression(exp) =>
        convertIntervalExpression(exp)(nodeMap, parent)
    }
  }
}

