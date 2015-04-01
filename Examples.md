[full source](http://code.google.com/p/graph-expression/source/browse/trunk/src/test/java/com/myml/gexp/examples/ExamplesTest.java)

## Person Extraction Example ##

Following code will extract those person chunks:
"Bill Smith | Devid Kent, Sr. | Devid M. Donald | Jonny K | Mr. Ahuna Edwards"
from text
```
    String text = " Mr. Ahuna Edwards is first person. " +
                        "Devid Kent, Sr. is second person. " +
                        "Devid M. Donald is just another person. " +
                        "Bill Smith is person with first name. Jonny K said: I'm happy.";

```

```
 public Chunker createPersonChunker() {
                GraphRegExp.Matcher Token = match("Token");
                GraphRegExp.Matcher Honorific =  G.regexp("^(Dr|Prof|Ms|Mrs|Mr)$",Token);
                GraphRegExp.Matcher CapitalizedWord = G.regexp("^[A-Z]\\w+$",Token);
                GraphRegExp.Matcher PersonSuffix = G.regexp("^(Jr|Sr)$",Token);
                GraphRegExp.Matcher CapitalLetter = G.regexp("^[A-Z]$",Token);
                GraphRegExp.Matcher FirstName = G.regexp("^(Bill|John)$",Token);
                GraphRegExp.Matcher Dot = match("Dot");
                GraphRegExp.Matcher Comma = match("Comma");
                GraphRegExp.Matcher PersonVerbs = G.regexp("^(said|met|walked)$",Token);


                GraphRegExp.Matcher commonPart = seq(CapitalizedWord, or(CapitalizedWord, seq(CapitalLetter, Dot)));
                Chunker chunker = Chunkers.pipeline(
                        Chunkers.regexp("Token", "\\w+"),
                        Chunkers.regexp("Dot", "\\."),
                        Chunkers.regexp("Comma", ","),
                        new GraphExpChunker(null,
                                or(
                                        mark("Person", or(
                                                seq(Honorific, opt(Dot), commonPart),
                                                seq(FirstName, CapitalizedWord),
                                                seq(commonPart, Comma, PersonSuffix, opt(Dot)),
                                                seq(CapitalizedWord, CapitalLetter, Dot, CapitalizedWord))
                                        ),
                                        seq(mark("Person", commonPart), PersonVerbs)
                                )
                        ).setDebugString(true)
                );
                return chunker;
        }
```

  * First we create helper clunkers: Token, Dot, Comma.
  * Then we create subset of Token chunks which satisfy predicate regexps those are: CapitalizedWord, PersonSuffix etc.
  * We write several Person patterns and test them
  * Thats all.

## Predicate Usage Example ##

Suppose we want to mark all IP address.
I could write "(\\d+\\.){4}" to do so, but then we'd have 333.333.333.333 marked as IP. Number from 0 to 255 in regular expressions will look like
```
Pattern.compile ("([1-9] | [1-9] [0-9] | 1 [0-9] [0-9] | 2 ([0-4] [0-9] | 5 [0-5 ]))");
```
predicates can avoid such complexity.
Graph-ehpression allows you to specify predicates over their patterns, that's how it looks
```
 GraphRegExp.Matcher Token = match("Token");
                GraphRegExp.Matcher Number = G.regexp("^\\d+$", Token);

                GraphRegExp.Matcher NotNumber = G.regexp("^[^\\d]+$", Token);
                class NumberPredicate implements Predicate<GraphMatchWrapper> {
                        int min;
                        int max;

                        NumberPredicate(int min, int max) {
                                this.min = min;
                                this.max = max;
                        }

                        public boolean evaluate(GraphMatchWrapper graphMatchWrapper) {
                                Integer number = Integer.parseInt(graphMatchWrapper.getText());
                                return number >= min && number <= max;
                        }
                }


                Chunker ipChunker = Chunkers.pipeline(
                        Chunkers.regexp("Token", "\\w+"),
                        new GraphExpChunker(null,
                                seq(
                                        NotNumber,
                                        mark("IpAddress", seq(
                                                match(Number, new NumberPredicate(1, 255)),
                                                times(match(Number, new NumberPredicate(0, 255)), 3, 3)
                                        )),
                                        NotNumber
                                )
                        )
                );
```

Using following text
```
      String text = "some text 123.255.888.555 some text " +
                        "255.255.1.1 some text 1.1.255.134 some text 1.1.1.1.1.1.1.1.1";
```
we will mark only
1.1.255.134 | 255.255.1.1

## Using weighted automaton ##
WeightedRegExpExample