

     ███╗   ██╗███████╗███████╗██████╗
     ████╗  ██║██╔════╝██╔════╝██╔══██╗
     ██╔██╗ ██║█████╗  █████╗  ██████╔╝
     ██║╚██╗██║██╔══╝  ██╔══╝  ██╔══██╗
     ██║ ╚████║██║     ███████╗██║  ██║
     ╚═╝  ╚═══╝╚═╝     ╚══════╝╚═╝  ╚═╝

     Version 1.0.1, June 7 - 2021
     A stable version of nfer appeared in 2016.
     Only minor updates have occurred since.

## Copyright statement

Copyright (c) 2021 California Institute of Technology (“Caltech”). U.S. Government sponsorship acknowledged.

## Disclaimer

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

## License

[APACHE LICENSE, VERSION 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)

## Introduction

nfer is a Scala package for generating abstractions from event traces.
nfer allows to define intervals over an event trace. The intervals indicate abstractions
that for example can be visualized. An interval consists of a name, two time stamps:
the beginning and the end of the interval, and a map from variable names (strings)
to values, indicating data that the interval carries.

The nfer system is described in detail in this [paper](papers/nfer-fmsd-2017.pdf).

## Writing Specifications

As an example, consider a spacecraft scenario where we want to be informed if
a downlink operation occurs during a 5-minute
time interval where the flight computer reboots twice.
This scenario could cause a potential loss of downlink information.
We want to identify the following intervals (informally):

- A _boot_ represents an interval during which the rover software is rebooting.
- A _double boot_ represents an interval during which the
  rover boots twice within a 5-minute timeframe.
- A _risk_ represents an interval during which the rover boots twice,
  and during which it also attempts to downlink information.

Our objective now is to **formalize** the definition of such intervals
in the nfer specification language, and extract these intervals from
a telemetry stream, based on such a specification.
Specifically, in this case, we need a formalism for formally defining
the following three kinds of intervals.

- A `BOOT` interval starts with a `BOOT_S` (boot start) event and ends with
  a `BOOT_E` (boot end) event. The `BOOT_S` and `BOOT_E` events are assumed to potentially 
  occur in the trace.

- A `DBOOT` (double boot) interval consists of two consecutive `BOOT` intervals,
with no more than 5-minutes from the end of the first `BOOT` interval to
the start of the second `BOOT` interval.

- A `RISK` interval is a `DBOOT` interval during which a `DOWNLINK` occurs.
  The `DOWNLINK` event is assumed to potentially occur in the trace.

The following time line (with time stamps) illustrates a scenario with two boots,
hence a double boot, within 5 minutes, and a downlink occurring during this
period.

```
12923    13112    20439     27626        48028
  |--------|--------|---------|------------|---------------->
BOOT_S   BOOT_E     |       BOOT_S       BOOT_E
                    |
                    |
                 DOWNLINK
```

The intervals can be formalized with the following nfer specification:

```
BOOT :- BOOT_S before BOOT_E
   map {count -> BOOT_S.count}

DBOOT :- b1:BOOT before b2:BOOT
   where b2.begin - b1.end <= 300000
   map {count -> b1.count}

RISK :- DOWNLINK during DBOOT
   map {count -> DBOOT.count}
```

The specification consists of three rules, one for each generated
interval: the name of the generated interval occurs before the
`:-` sign. The rules are to be read as follows. 

- A `BOOT` interval is created if a `BOOT_S` event occurs before a
  `BOOT_E` event. In addition the `count` data from the first `BOOT_S` event is
  inherited to become part of the `BOOT` interval.
- Likewise, a `DBOOT` (double boot) interval is created when observing
  a `BOOT` interval occurring before another `BOOT` interval with no more
  than 5 minutes (300,000 milliseconds) in between. 
- Finally, a `RISK` interval is created when observing a
  `DOWNLINK` during a `DBOOT` (double boot) interval.

The nfer specification language also allows rules to be presented in modules,
which can import other modules. The specification above can alternatively
be presented as follows.

```
module Booting {
  BOOT :- BOOT_S before BOOT_E
    map {count -> BOOT_S.count}
}

module DoubleBooting {
  import Booting;

  DBOOT :- b1:BOOT before b2:BOOT
    where b2.begin - b1.end <= 300000
    map {count -> b1.count}
}

module Risking {
  import DoubleBooting;

  RISK :- DOWNLINK during DBOOT
    map {count -> DBOOT.count}
}
```

In a modular specification it is the last module which becomes the effective one,
as well as all the modules that it imports transitively.

## Applying a Specification

The following Scala program illustrates how a specification is applied to
a trace. After an instance of the `Nfer` class has been created,
it can be initiated with a specification, for example read from a text file
(a specification can also be provided directly as a text string). Subsequently
events can be submitted to the monitor.

```scala
object Example {
  def main(args: Array[String]): Unit = {
    val nfer = new Nfer()
  
    nfer.specFile("somePath/spec.nfer")
  
    nfer.submit("BOOT_S", 12923, "count" -> 42)
    nfer.submit("BOOT_E", 13112)
    nfer.submit("DOWNLINK", 20439)
    nfer.submit("BOOT_S", 27626, "count" -> 43)
    nfer.submit("BOOT_E", 48028)
  }
}
```


## The nfer Grammar

The grammar for the nfer specification language is provided below. 

The meta symbols used are as follows.

- `X*` means zero or more `X` 
- `X+` means one or more `X`.
- `X,*` means zero or more `X` separated by commas. 
- `X,+` means one or more `X` separated by commas. 
- `[X]` means zero or one `X`.
- `X|Y` means `X` or `Y`.
- `';'` means a symbol, in this case `;`. Also used for keywords.
- `<spec>` means a non-terminal, in this case that of specifications.
- `X ::= ...` defines the non-terminal `X`.
- `int-<exp>` means `<exp>`, but with the informal annotation that it must be an integer.

```
<spec> ::= 
    <rule>+ 
  | <module>+

<module> ::= 'module' <ident> '{' [<imports>] <rule>* '}'

<imports> ::= 'import' <ident>,+ ';'

<rule> ::= <ident> ':-' <interval> [<where>] [<map>] [<endPoints>]

<interval> ::= <primaryInterval> (<intervalOp> <primaryInterval>)*

<primaryInterval> ::= <atomicInterval> | <parenInterval>

<atomicInterval> ::= [<label>] <ident>

<parenInterval> := '(' <interval> ')'

<label> ::= <ident> ':'

<where> ::= 'where' bool-<exp>

<map> ::= 'map' '{' (<ident> '->' <exp>),+ '}'

<endPoints> ::= 'begin' int-<exp> 'end' int-<exp>

<intervalOp> ::= 
   'also' 
 | 'before' 
 | 'meet' 
 | 'during' 
 | 'start' 
 | 'finish' 
 | 'overlap' 
 | 'slice' 
 | 'coincide'

<exp> ::=
    <unaryOp> <exp> |
  | <exp> <binaryOp> <exp>
  | <stringLit>
  | <natLit>
  | <doubleLit>
  | <boolLit>
  | <beginTime>
  | <endTime>
  | <mapField>
  | <call>
  | '(' <exp> ')'

<unaryOp>  ::= '-' | '!'

<binaryOp> ::= '&' | '|' | '+' | '-' | '*' | '/' | '%' | '<' | '<=' | '>' | '>=' | '=' | '!='

<stringLit> ::= """"[^"]*"""".r

<natLit> ::= """(\d+)""".r

<doubleLit> ::= """(\d+\.\d*|\d*\.\d+)([eE][+-]?\d+)?[fFdD]?""".r

<boolLit> ::= 'true' | 'false'

<beginTime> ::= <ident> '.' 'begin'

<endTime> ::= <ident> '.' 'end'

<mapField> ::= <ident> '.' <ident>

<call> ::= <ident> '(' <exp>,* ')'
```

## Installing DejaVu:

The directory ``out`` contains the nfer jar file:

* out/artifacts/nfer_jar/nfer.jar 

nfer is implemented in Scala. Install the Scala programming language if not already installed (https://www.scala-lang.org/download). nfer is implemented in Scala 2.13.5.

## Papers

A thorough explanation of nfer, and its algorithms can be found in the following paper:

[Inferring Event Stream Abstractions](papers/nfer-fmsd-2017.pdf)
Sean Kauffman, Klaus Havelund, Rajeev Joshi, and Sebastian Fischmeister.

## Developers

The nfer system was conceptualized and developed by (in alphabetic order): Klaus Havelund, Rajeev Joshi, and Sean Kauffman during Sean Kauffman's internship at JPL.
