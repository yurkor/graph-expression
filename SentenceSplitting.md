### Keywords: Sentence splitting, Sentence boundary detection, Sentence extraction ###

Here is example of how sentence splitting can be written in GExp.
The idea is short subset of heuristic model in lingpipe (http://alias-i.com/lingpipe/docs/api/com/aliasi/sentences/IndoEuropeanSentenceModel.html).
It defines possible stop tokens, and some restriction on before and after tokens which negotiates some stops.
Token chunk is different from previous examples, because now we should work with non word characters sequence.

Input:
```
  String data = "A small letter after dot, for example something. means that the sentence is not finished.\n" +
                        "Sentence (can not be unbalanced by brackets. Because this part is also joined).\n" +
                        "Here many acronyms A. B. C. and then end.\n" +
                        "And there's no dot, but it's the last";
```

Output:
```
Sentence[A small letter after dot, for example something. means that the sentence is not finished.]

Sentence[Sentence (can not be unbalanced by brackets. Because this part is also joined).]
.\nSe
Sentence[Here many acronyms A. B. C. and then end.]
.\nHe
Sentence[And there's no dot, but it's the last]
.\nAn

```

And chunker definition:
```
public Chunker createSentenceChunker() {
                GraphRegExp.Matcher Token = match("Token");

                //characters that can lead to sentence stop
                GraphRegExp.Matcher PossibleStop = regexp("^(\\.{1,2}|!|\\?|\\\\|\\)\\.|\")$", Token);
                //abbreviation can't lead to sentence stop
                GraphRegExp.Matcher ImpossiblePrefix = regexp("^([A-Z]\\w{0,2})$", Token);
                //can start with lowercase or some other characters
                GraphRegExp.Matcher ImpossibleSuffix = or(regexp("^([a-z].*|[\\]\\[\\}]+)$", Token), PossibleStop);


                Predicate<GraphMatchWrapper> balancedParentnesPredicate = new Predicate<GraphMatchWrapper>() {
                        @Override
                        public boolean evaluate(GraphMatchWrapper match) {
                                String text = match.getText();
                                return sameCount(text, "(", ")") && sameCount(text, "[", "]") && sameCount(text, "{", "}");
                        }

                        public boolean sameCount(String str, String char1, String char2) {
                                return countMatches(str, char1) == countMatches(str, char2);
                        }
                };
                Chunker chunker = Chunkers.pipeline(
                        Chunkers.regexp("Token", "(\\b\\w+\\b|[^\\s\\w]+)"),
                        new GraphExpChunker(null,
                                seq(
                                        GraphUtils.lookahead(false, Token, ImpossiblePrefix),
                                        mark("Stop", PossibleStop),
                                        GraphUtils.lookahead(false, Token, ImpossibleSuffix)


                                )
                        ),
                        new GraphExpChunker("Sentence",
                                match(seq(
                                        times(Token, 3, 100).reluctant(),
                                        or(match("Stop"), match(GraphExpChunker.END))
                                ), balancedParentnesPredicate)
                        )
                );
                return chunker;
        }
```