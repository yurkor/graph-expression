# GExp #
High level automaton library for information extraction.
Usage:
  * Named Entity Recognition(NER) patterns
  * Optimal match finding (for ambiguous grammars)
  * Relation and fact extraction
  * Structure parsing (like document structure, sentence parsing)
  * Search problem solving (like Suduku)

# GExp Features: #

  * **all standard** reg exp operators: +, {n,n}, {n,}, ....
  * **much more**: java predicates on groups, reluctant, greedy, cut operator, inner match...
  * **structure of match**  -it is possible to build a syntax tree based on match
  * **weighted regexps** allow you to encode preference of different graph path with your scoring functions, heuristic search will handle the rest.
  * **easy extendable** framework model. You can easily write new matchers and use them with the existing ones.
  * **dynamic structure** of graph (allow you to use gexp to solve search problems like suduko, 8-qeen problems)
  * **fast** - it works faster than Jape transducer (gate.ac.uk), the closest project to this one
  * **scopes** for variables: all you predicates can set/get variables from current scope/context to do their job
  * easily **embeddable** - a few line of java code and you can use power of graph-expression in you project.

Read more on wiki
https://github.com/yurkor/graph-expression/blob/wiki/ProjectHome.md
