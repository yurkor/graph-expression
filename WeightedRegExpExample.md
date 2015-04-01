## Using weighted automaton ##
graph-expression allow you to write your automaton with predefined weight in that case it will use heuristic search to find optimal path through you automaton. It's easy!

In next example we would like to annotate best person from whole text, only one person (similar tasks can be found in real live for detection of main location in article).

Our simple rules:
  * person must start with Honorific (Mr)
  * Preferably for the person's name consisted of capitalized words. [weight:0.1, otherwise 1](Capital.md)
  * we don't want to annotate whole text as tokens so token weight is 0.2 (Capital word will be preferred because it is 0.1)

Rule will look like:
  * main matcher is selectBest which is optimal path searcher of its children matchers.
  * "tokens" matcher weight tokens under itself and return weight to parent selectBest
  * We will anotate person with another selectBest which will select person after some position, and return best first. It should be wrapped by weightedBySum so top level selectBest will know score of inner matches(by default weight of all matches is 0)

Here full code which will return Mr John Smith, and if there are no such long name it will return second best answer Mr adam.

```
       String data = "Why is the programmer Mr adam better than programmer Mr John Smith?";
                TextWithChunks textWithChunks = new TextWithChunks(data);
                GraphRegExp.Matcher upper = group("upper", GraphUtils.regexp("^[A-Z]", match("Token")));
                GraphRegExp.Matcher mr = GraphUtils.regexp("^Mr$", match("Token"));
                GraphRegExp.Matcher token = group("lower", match("Token"));
                Transformer<GraphMatchWrapper, Double> length = new Transformer<GraphMatchWrapper, Double>() {
                        public Double transform(GraphMatchWrapper m) {
                                return m.getChunksList().size() * 0.2;
                        }
                };
                GraphRegExpExt.WeightedMatcher tokens = weighted(star(token).relucant(), length).setShowBestFist(true);
                GraphExpChunker personChunker = new GraphExpChunker(null,
                        selectBest(
                                match(GraphUtils.START),
                                tokens,
                                weightedBySum(mark("person", seq(mr,
                                        selectBest(weighted(or(upper, token), withGroupScorer("upper", 0.1, 1)), 1, 10)
                                ))).setShowBestFist(true),
                                tokens,
                                match(GraphUtils.END)
                        )
                );
                Chunker pipeline = Chunkers.pipeline(
                        Chunkers.regexp("Token", "\\w+"),
                        personChunker
                );
                Chunkers.execute(pipeline, textWithChunks);
                assertEquals("person[Mr John Smith]\n" +
                        "n Mr\n", Chunkers.toChunksString(textWithChunks, 2, false, "person"));
```