This example shows how to extract USA Addresses or PO Boxes only with all fields including Postcode, Street Address, State and country.
Examples were taken from http://www.bitboost.com/ref/international-address-formats/united_states/

Chunker code:
```
   GraphRegExp.Matcher Token = match("Token");
                GraphRegExp.Matcher Country = GraphUtils.regexp("^USA$", Token);
                GraphRegExp.Matcher Number = GraphUtils.regexp("^\\d+$", Token);
                GraphRegExp.Matcher StateLike = GraphUtils.regexp("^([A-Z]{2})$", Token);
                GraphRegExp.Matcher Postoffice = seq(match("BoxPrefix"), Number);
                GraphRegExp.Matcher Postcode =
                                mark("Postcode", seq(GraphUtils.regexp("^\\d{5}$", Token), opt(GraphUtils.regexp("^\\d{4}$", Token))))
                        ;
//mark(String, Matcher) -- means creating chunk over sub matcher
                GraphRegExp.Matcher streetAddress = mark("StreetAddress", seq(Number, times(Token, 2, 5).reluctant()));
                //without new lines
                streetAddress = regexpNot("\n", streetAddress);
                GraphRegExp.Matcher City = mark("City", GraphUtils.regexp("^[A-Z]\\w+$", Token));

                Chunker chunker = Chunkers.pipeline(
                        Chunkers.regexp("Token", "\\w+"),
                        Chunkers.regexp("BoxPrefix", "\\b(POB|PO BOX)\\b"),
                        new GraphExpChunker("Address",
                                seq(
                                        opt(streetAddress),
                                        opt(Postoffice),
                                        City,
                                        StateLike,
                                        Postcode,
                                        Country
                                )
                        ).setDebugString(true)
                );
```

InputString:
```
    String SPLITTER = "\n some other words dsfds TND USA street \n";
                String data = "CHRIS NISWANDEE\n" +
                        "   SMALLSYS INC\n" +
                        "   795 E DRAGRAM\n" +
                        "   TUCSON AZ 85705-7598\n" +
                        "   USA\n" +
                        SPLITTER +
                        "MARY ROE\n" +
                        "   MEGASYSTEMS INC\n" +
                        "   SUITE 5A-1204\n" +
                        "   799 E DRAGRAM\n" +
                        "   TUCSON AZ 85705\n" +
                        "   USA\n" +
                        SPLITTER +
                        "JANE ROE\n" +
                        "   200 E MAIN ST\n" +
                        "   PHOENIX AZ 85123\n" +
                        "   USA\n" +
                        SPLITTER +
                        "JOHN SMITH\n" +
                        "   100 MAIN ST\n" +
                        "   PO BOX 1022\n" +
                        "   SEATTLE WA 98104\n" +
                        "   USA"
                        + SPLITTER
                        + " CHRIS NISWANDEE\n" +
                        "   BITBOOST\n" +
                        "   POB 65502\n" +
                        "   TUCSON AZ 85728\n" +
                        "   USA";
```

Output: //run test and see it for yourself
```
     assertEquals("Address[795 E DRAGRAM\\n   TUCSON AZ 85705-7598\\n   USA]\n" +
                        "  79\n" +
                        "Address[799 E DRAGRAM\\n   TUCSON AZ 85705\\n   USA]\n" +
                        "  79\n" +
                        "Address[200 E MAIN ST\\n   PHOENIX AZ 85123\\n   USA]\n" +
                        "  20\n" +
                        "Address[100 MAIN ST\\n   PO BOX 1022\\n   SEATTLE WA 98104\\n   USA]\n" +
                        "  10\n" +
                        "Address[POB 65502\\n   TUCSON AZ 85728\\n   USA]\n" +
                        "  PO\n", Chunkers.toChunksString(textWithChunks, 2, false, "Address"));
```