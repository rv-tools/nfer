

     ███╗   ██╗███████╗███████╗██████╗
     ████╗  ██║██╔════╝██╔════╝██╔══██╗
     ██╔██╗ ██║█████╗  █████╗  ██████╔╝
     ██║╚██╗██║██╔══╝  ██╔══╝  ██╔══██╗
     ██║ ╚████║██║     ███████╗██║  ██║
     ╚═╝  ╚═══╝╚═╝     ╚══════╝╚═╝  ╚═╝

        Version 1.0.0, June 7 - 2021

## Copyright statement

Copyright (c) 2021 California Institute of Technology (“Caltech”). U.S. Government sponsorship acknowledged.

## Disclaimer

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

## License

[APACHE LICENSE, VERSION 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)

## Overview

nfer is a program written in Scala for generating intervals from an event stream stored in a log file according to a user provided specification consisting of interval producing rules. As an example, the following specification consists of 
three rules:

```
boot :- boot_s before boot_e

dboot :- b1:boot before b2:boot where (b2.end - b1.begin) < 300

risk :- downlink during dboot
```

Each rule has the form:

```
name :- body
```

The first rule generates a `boot` interval upon detecting a `boot_s` (boot start) event followed by (`before`) a `boot_e` (boot end) event
in the log. The second rule generates a double boot (`dboot`) event upon detecting a `boot` followed by a `boot`
with less than 300 milliseconds between the end of the first `boot` and the start of the second `boot`.
The third rule produces a `risk` interval if there occurs a `downlink` event during a `dboot` interval.

## Installing DejaVu:

The directory ``out`` contains the following necessary for installing and running nfer:

* nfer                         : script to run the system
* artifacts/nfer_jar/nfer.jar  : the nfer jar file

nfer is implemented in Scala. To install nfer do the following.

1. Install the Scala programming language if not already installed (https://www.scala-lang.org/download). nfer is implemented in Scala 2.13.5.
2. Place the files ``nfer`` and ``nfer.jar`` mentioned above in some directory **DIR** (standing for the total path to this directory).
3. cd to  **DIR** and make the script executable:

        chmod +x nfer

4. Preferably define an alias in your shell profile to the dejavu script so it can be called from anywhere:

        export PATH=DIR:$PATH

## Running nfer

The script is applied as follows:

    nfer <specFile> <logFile> true|false true|false

- <specFile> is the relative path to the specification
- <traceFile> is the relative path to the trace (log file)
- Then follows an indication of whether to run nfer in minimality mode (true) or not (false)
- Then follows an indication of whethe to print out all generated intervals at the end (true) or not (false)


**The specification file** (``<specFile>``) should follow the following grammar:

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

**The log file** (``<logFile>``) should be a JSON file, an example of which is shown below.

```json
{"execution": 
  [ 
    ["boot_s",   10],
    ["boot_e",   14],
    ["downlink", 22],
    ["boot_s",   50],
    ["boot_e",   60],
  ]
}
```

## Papers

[Inferring Event Stream Abstractions](papers/nfer-fmsd-2017.pdf)
Sean Kauffman, Klaus Havelund, Rajeev Joshi, and Sebastian Fischmeister.

## Authors

The nfer system was conceptualized and developed by (in alphabetic order): Klaus Havelund, Rajeev Joshi, and Sean Kauffman.
