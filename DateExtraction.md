Date Extraction example:
input
```
 String data = Joiner.on(SPLITTER).join(
                        "12 June 1983",   //ok
                        "133 June 2000", //fail
                        "12 Aug 09",           //ok
                        "12/12/09",                //ok
                        "12.13.09",//ok
                        "1.May.09",      //ok
                        "May 12 1983" //ok, month day order is not important
                );
```

```
  public Chunker createDateTimeChunker() {
                class Util {
                        GraphRegExp.Matcher number(final int min, final int max) {
                                return match(GraphUtils.regexp("^\\d+$", match("Token")), new Predicate<GraphMatchWrapper>() {
                                        @Override
                                        public boolean evaluate(GraphMatchWrapper graphMatchWrapper) {
                                                int value = Integer.parseInt(graphMatchWrapper.getText());
                                                return value >= min && value <= max;
                                        }
                                });
                        }
                }
                Util u = new Util();
                GraphRegExp.Matcher Token = match("Token");
                GraphRegExp.Matcher Month = or(regexp("^(June?|July?|Aug(ust)?|May)$", Token), u.number(1, 12));
                GraphRegExp.Matcher Year = or(regexp("\\w{2}", u.number(1, 99)), u.number(1900, 2100));
                GraphRegExp.Matcher Day = u.number(1, 31);


                Chunker chunker = Chunkers.pipeline(
                        Chunkers.regexp("Token", "\\w+"),
                        new GraphExpChunker("Date",
                                seq(
                                        or(seq(Month, Day), seq(Day, Month)),
                                        Year
                                )
                        ).setDebugString(false)
                );
                return chunker;
        }

        @Test
        public void testDateTime() {
                String SPLITTER = "\n some other words dsfds TND USA street \n";
                String data = Joiner.on(SPLITTER).join(
                        "12 June 1983",   //ok
                        "133 June 2000", //fail
                        "12 Aug 09",           //ok
                        "12/12/09",                //ok
                        "12.13.09",//ok
                        "1.May.09",      //ok
                        "May 12 1983" //ok
                );
                TextWithChunks textWithChunks = new TextWithChunks(data);
                Chunker pipeline = createDateTimeChunker();
                Chunkers.execute(pipeline, textWithChunks);
                assertEquals("Date[12 June 1983]\n" +
                        "\n" +
                        "Date[12 Aug 09]\n" +
                        " \\n12\n" +
                        "Date[12/12/09]\n" +
                        " \\n12\n" +
                        "Date[12.13.09]\n" +
                        " \\n12\n" +
                        "Date[1.May.09]\n" +
                        " \\n1.\n" +
                        "Date[May 12 1983]\n" +
                        " \\nMa\n", Chunkers.toChunksString(textWithChunks, 2, false, "Date"));
        }
```