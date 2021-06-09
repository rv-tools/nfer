package nfer.repr.nodes.node4

/******************/
/*   Algorithm 4  */
/* Rolling Window */
/******************/

/*
Rolling window only considers items in the cache that are within a time window. Items older
than the window threshold are discarded.
 */

import java.lang.Math._

import nfer.ast._
import nfer.repr.Util._
import nfer.repr._
import nfer.{Interval, Sclk}

// ------
// Nodes:
// ------

private[nfer] class Node {
  var inputNode1: Node = null
  // null in leaf nodes
  var inputNode2: Node = null
  // null in leaf nodes
  var outputNode: Node = null // null in top node

  /**
    * Used by the top node to evaluate constraints, end points, and map.
    */
  var rule: Rule = null
  /**
    * Used by top node to return resulting interval back to the agent.
    */
  var callBack: Interval => Unit = null

  var operator: String = this.getClass.getSimpleName.takeWhile(_ != 'N')
  // N is first letter in Node - we include the string before that.
  var number: Int = Node.nextNumber
  var name: IntervalName = Node.nextName
  var tokens: Set[Token] = Set()
  var tokensNew: Set[Token] = Set()
  val emptyTokenSet: Set[Token] = Set()
  val emptyData: Data = Map()
  val WINDOW: Int = 41000 // NEW <======== PARAMETER TO EXPERIMENT =========
  // 60 was used for the first version

  def inWindow(interval: Interval, now: Sclk): Boolean = { // NEW
    interval.end >= now - WINDOW
  }

  def setCallBack(cb: Interval => Unit): Unit = {
    callBack = cb
  }

  /**
    * Get all intervals in this node, and in all sub-nodes, walking
    * depth first search. Each node results in a set of intervals.
    * Can be used for testing purposes.
    */

  def getIntervals: List[Set[Interval]] = {
    val interval = tokens map (_.interval)
    val intervals1 = if (inputNode1 != null) inputNode1.getIntervals else Nil
    val intervals2 = if (inputNode2 != null) inputNode2.getIntervals else Nil
    List(interval) ++ intervals1 ++ intervals2
  }

  /**
    * Submitting interval to agent from outside agent.
    *
    * @param interval
    */

  def addInterval(interval: Interval): Unit = {
    require(inputNode1 == null && inputNode2 == null, "must be a leaf node")
    require(outputNode != null, "cannot be a top node")
    val token = new Token(interval, Map(name -> interval))
    tokens += token
    outputNode.addToken(token, this)
  }

  /**
    * Called by top node when an interval reaches it. Calls the callback function.
    *
    * @param interval
    */

  def publish(interval: Interval): Unit = {
    //println(s"Publishing $interval")
    if (callBack != null)
      callBack(interval)
  }

  /**
    * Submitting token from subnode to supernode.
    *
    * @param token
    * @param inputNode
    */

  def addToken(token: Token, inputNode: Node): Unit = {
    if (inputNode.eq (inputNode1)) {
      require(inputNode2 != null)
      leftActivate(token)
    }
    else if (inputNode.eq(inputNode2) && inputNode1 != null) {
      rightActivate(token)
    }
    else { // inputNode == inputNode2 && inputNode1 == null: atomic child
      tokensNew += token
    }
    for (token <- tokensNew) evaluateToken(token)
    tokensNew = emptyTokenSet
  }

  // TODO: not efficient (better data structure or GC)

  def leftActivate(leftToken: Token): Unit = {
    for (rightToken <- inputNode2.tokens) {
      applyTemporalOperator(leftToken, rightToken)
      if (!inWindow(rightToken.interval,leftToken.interval.end)) { // NEW
        inputNode2.tokens -= rightToken
      }
    }
  }

  def rightActivate(rightToken: Token): Unit = {
    for (leftToken <- inputNode1.tokens) {
      applyTemporalOperator(leftToken, rightToken)
      if (!inWindow(leftToken.interval,rightToken.interval.end)) { // NEW
        inputNode1.tokens -= leftToken
      }
    }
  }

  def applyTemporalOperator(token1: Token, token2: Token) {
    val Token(interval1, env1) = token1
    val Token(interval2, env2) = token2
    if (predefinedOrdered(interval1, interval2)) {
      val (begin, end) = predefinedEndPoints(interval1, interval2)
      val env = env1 ++ env2
      val newInterval = Interval(name, begin, end, emptyData)
      val newToken = Token(newInterval, env)
      tokensNew += newToken
    }
  }

  def evaluateToken(token: Token): Unit = {
    if (!MINIMALITY || minimal(token)) {
      if (outputNode != null) {
        tokens += token
        outputNode.addToken(token, this)
      } else {
        if (evaluateConstraint(token)) {
          val (begin, end) = evaluateEndPoints(token)
          val data = evaluateData(token)
          val newInterval = Interval(name, begin, end, data)
          val newToken = Token(newInterval, token.environment)
          tokens += newToken
          publish(newToken.interval)
        }
      }
    }
  }

  def minimal(token: Token): Boolean = {
    minimalIn(token, tokens) // && minimalIn(token, tokensNew)
  }

  def minimalIn(token: Token, tokenSet: Set[Token]): Boolean = // NEW
    !(tokenSet exists (smallerToken =>
      inWindow(smallerToken.interval,token.interval.end) &&
      token.interval.contains(smallerToken.interval)))

  // Evaluation of constraints, end points, and maps:

  def evaluateConstraint(token: Token): Boolean = {
    rule.constraint match {
      case None => true
      case Some(exp) =>
        evaluateExp(exp)(token).asInstanceOf[Boolean]
    }
  }

  def evaluateEndPoints(token: Token): (Sclk, Sclk) = {
    rule.endPoints match {
      case None => (token.interval.begin, token.interval.end)
      case Some((exp1, exp2)) =>
        val begin = evaluateExp(exp1)(token).asInstanceOf[Int].toDouble
        val end = evaluateExp(exp2)(token).asInstanceOf[Int].toDouble
        (begin, end)
    }
  }

  def evaluateData(token: Token): Data = {
    rule.mapping match {
      case None => Map()
      case Some(m) =>
        m map {
          case (name, exp) => (name -> evaluateExp(exp)(token))
        }
    }
  }

  // Evaluation of expressions

  def evaluateExp(exp: Expression)(token: Token): Any =
    ExpressionEvaluation.evaluate(exp)(token)

  // The following methods should be overridden by subclasses:

  protected def predefinedOrdered(interval1: Interval, interval2: Interval): Boolean = {
    true
  }

  protected def predefinedEndPoints(interval1: Interval, interval2: Interval): (Sclk, Sclk) = {
    val begin = min(interval1.begin, interval2.begin)
    val end = max(interval1.end, interval2.end)
    (begin, end)
  }

  // Printing:

  override def toString: String = {
    var result = ""
    if (outputNode == null) {
      // top node
      result += "digraph g {\n"
      result += "\n"
      result += "  graph [rankdir = \"LR\"]\n"
      result += "  node[fontsize = \"16\", shape = \"record\"]\n"
      result += "\n"
      result += toStringContents
      result += "}"
    } else {
      result += toStringContents
    }
    result
  }

  def toStringContents: String = {
    var result = ""
    result += s"""  node$number[label="$number:$name $operator"""
    result += tokens.mkString("|", "|", "")
    result += "\"]\n"
    if (outputNode != null) {
      result += s"  node$number -> node${outputNode.number}\n"
    }
    result += "\n"
    if (inputNode1 != null) result += inputNode1.toString
    if (inputNode2 != null) result += inputNode2.toString
    result
  }
}
