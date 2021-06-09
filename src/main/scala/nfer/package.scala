/**
  *
  * '''nfer''' is a Scala package for generating abstractions from ''event traces''.
  *
  * == Introduction ==
  *
  * nfer allows to define intervals over an event trace. The intervals indicate abstractions
  * that for example can be visualized. An interval consists of a name, two time stamps:
  * the beginning and the end of the interval, and a map from variable names (strings)
  * to values, indicating data that the interval carries.
  *
  * == Writing Specifications ==
  *
  * As an example, consider a scenario where we want to be informed if
  * a downlink operation occurs during a 5-minute
  * time interval where the flight computer reboots twice.
  * This scenario could cause a potential loss of downlink information.
  * We want to identify the following intervals.
  *
  * A BOOT represents an interval during which the rover software is rebooting.
  *
  * A DBOOT (double boot) represents an interval during which the
  * rover reboots twice within a 5-minute timeframe.
  *
  * A RISK represents an interval during which the rover reboots twice,
  * and during which also attempts to downlink information.
  *
  * Our objective now is to formalize the definition of such intervals
  * in the nfer specification language, and extract these intervals from
  * a telemetry stream, based on such a specification.
  * Specifically, in this case, we need a formalism for formally defining
  * the following three kinds of intervals.
  *
  * A BOOT interval starts with a BOOT_S (boot start) event and ends with
  * a BOOT_E (boot end) event.
  *
  * A DBOOT (double boot) interval consists of two consecutive BOOT intervals,
  * with no more than 5-minutes from the end of the first BOOT interval to
  * the start of the second BOOT interval.
  *
  * A RISK interval is a DBOOT interval during which a DOWNLINK occurs.
  *
  * The following time line illustrates a scenario with two BOOTs,
  * hence a double boot, and a downlink occurring during this
  * period.
  *
  * {{{
  * -----|---------|------------|---------|------->
  *   BOOT_S   BOOT_E   |   BOOT_S     BOOT_E
  *                     |
  *                  DOWNLINK
  * }}}
  *
  * The intervals can be formalized with the following nfer specification:
  *
  * {{{
  * BOOT :- BOOT_S before BOOT_E
  *   where BOOT_E.begin - BOOT_S.end <= 5000
  *   map {count -> BOOT_S.count}
  *
  * DBOOT :- b1:BOOT before b2:BOOT
  *   where b2.begin - b1.end <= 10000
  *   map {count -> b1.count}
  *
  * RISK :- DOWNLINK during DBOOT
  *   map {count -> DBOOT.count}
  *
  * }}}
  *
  * The specification consists of three rules, one for each generated
  * interval: the name of the generated interval occurs before the
  * :- sign. The rules are to be read as follows. A BOOT interval is
  * created if a BOOT_S event occurs before a
  * BOOT_E event. In addition the count data from the first BOOT_S event is
  * inherited to become part of the BOOT interval.
  *
  * Likewise, a DBOOT (double boot) interval is created when observing
  * a BOOT interval occurring before another BOOT interval with no more
  * than 300 seconds in between. Finally, a RISK interval is created when observing a
  * DOWNLINK during a DBOOT (double boot) interval.
  *
  * The nfer specification language also allows rules to be presented in modules,
  * which can import other modules. The specification above can alternatively
  * be presented as follows.
  *
  * {{{
  * module Booting {
  *   BOOT :- BOOT_S before BOOT_E
  *     where BOOT_E.begin - BOOT_S.end <= 5000
  *     map {count -> BOOT_S.count}
  * }
  *
  * module DoubleBooting {
  *   import Booting;
  *
  *   DBOOT :- b1:BOOT before b2:BOOT
  *     where b2.begin - b1.end <= 10000
  *     map {count -> b1.count}
  * }
  *
  * module Risking {
  *   import DoubleBooting;
  *
  *   RISK :- DOWNLINK during DBOOT
  *     map {count -> DBOOT.count}
  * }
  * }}}
  *
  * In a modular specification it is the last module which becomes the effective one,
  * as well as all the modules that it imports transitively.
  *
  * == Applying a Specification ==
  *
  * The following Scala program illustrates how a specification is applied to
  * a trace. After an instance of the Nfer class has been instantiated,
  * it can be initiated with a specification, for example read from a text file
  * (a specification can also be provided directly as a text string). Subsequently
  * events can be submitted to the monitor.
  *
  * {{{
  * object Test1 {
  *   def main(args: Array[String]): Unit = {
  *     val nfer = new Nfer()
  *
  *     val fileSpec = "somePath/spec1.nfer"
  *     nfer.specFile(fileSpec)
  *
  *     nfer.submit("BOOT_S",1000,"count" -> 42)
  *     nfer.submit("BOOT_E",2000)
  *     nfer.submit("BOOT_S",3000, "count" -> 43)
  *     nfer.submit("DOWNLINK",4000)
  *     nfer.submit("BOOT_E",5000)
  *   }
  * }
  * }}}
  *
  *
  * == The nfer Grammar ==
  *
  * The grammar for the nfer specification language is provided below. The meta symbols used are
  * as follows.
  * X* means one or more X whereas X+ means one or more.
  * [X] means zero or one X.
  * X|Y means X or Y.
  * ';' means the symbol ;.
  * specification is a non-terminal.
  * X ::= ... defines the non-terminal X.
  *
  * {{{
  * specification ::= rule+ | module+
  * module ::= 'module' identifier '{' [imports] rule* '}'
  * imports ::= 'import' identifier (',' identifier)* ';'
  * rule ::= identifier ':-' intervalExpression [whereExpression] [mapExpression] [endPoints]
  * intervalExpression ::= primaryIntervalExpression (intervalOp primaryIntervalExpression)*
  * primaryIntervalExpression ::= atomicIntervalExpression | parenIntervalExpression
  * atomicIntervalExpression ::= [label] identifier
  * parenIntervalExpression ::= '(' intervalExpression ')'
  * label ::= identifier ':'
  * whereExpression ::= 'where' expression
  * mapExpression ::= 'map' '{' identifier '->' expression (',' identifier '->' expression)* '}'
  * endPoints ::= 'begin' expression 'end' expression
  * intervalOp ::= 'also'|'before'|'meet'|'during'|'start'|'finish'|'overlap'|'slice'|'coincide'
  * expression ::= comparisonExpression (andorOp comparisonExpression)*
  * comparisonExpression ::= plusminusExpression (comparisonOp plusminusExpression)*
  * plusminusExpression ::= muldivExpression (plusminusOp muldivExpression)*
  * muldivExpression ::= unaryExpression (muldivOp unaryExpression)*
  * unaryExpression ::=
  * stringLiteral | intLiteral | doubleLiteral | booleanLiteral |
  * beginTime | endTime | mapField | callExpression |
  * '(' expression ')' | unaryOp unaryExpression
  * mapField ::= identifier '.' identifier
  * beginTime ::= identifier '.' BEGIN
  * endTime ::= identifier '.' END
  * unaryOp ::= '-' | '!'
  * muldivOp ::= '*'  | '/'  | '%' )
  * plusminusOp ::= '+'  | '-'
  * comparisonOp ::= '<' | '<=' | '>' | '>=' | '=' | '!='
  * andorOp ::= '&' | '|'
  * callExpression ::= identifier '(' expression (',' expression)* ')'
  * }}}
  */

package object nfer {
  type Sclk = Double
}

