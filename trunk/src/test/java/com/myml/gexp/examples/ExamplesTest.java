package com.myml.gexp.examples;

import com.google.common.base.Joiner;
import com.myml.gexp.chunker.Chunk;
import com.myml.gexp.chunker.Chunker;
import com.myml.gexp.chunker.Chunkers;
import com.myml.gexp.chunker.TextWithChunks;
import com.myml.gexp.chunker.common.GraphExpChunker;
import com.myml.gexp.chunker.common.GraphMatchWrapper;
import com.myml.gexp.chunker.common.typedef.GraphUtils;
import com.myml.gexp.graph.matcher.GraphRegExp;
import com.myml.gexp.graph.matcher.GraphRegExpExt;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;
import org.junit.Assert;
import org.junit.Test;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static com.myml.gexp.chunker.common.typedef.GraphUtils.*;

/**
 * Author: java developer 1
 * Date: 04.02.2011
 * Time: 17:14:51
 */
public class ExamplesTest extends Assert {

        /**
         * 1. @Honorific CapitalizedWord CapitalizedWord
         * a. @Honorific is a list of honorific titles such as {Dr., Prof., Mr., Ms., Mrs.
         * etc.)
         * b. Example: Mr. John Edwards
         * <p/>
         * 2. @FirstNames CapitalizedWord
         * a. @FirstNames is a list of common first names collected from sites like the
         * U.S. census and other relevant sites
         * b. Example: Bill Hellman
         * <p/>
         * 3. CapitalizedWord CapitalizedWord [,] @PersonSuffix
         * a. @PersonSuffix is a list of common suffixes such as {Jr., Sr., II, III, etc.}
         * b. Example: Mark Green, Jr.
         * <p/>
         * 4. CapitalizedWord CapitalLetter [.] CapitalizedWord
         * a. CapitalLetter followed by an optional period is a middle initial of a person
         * and a strong indicator that this is a person name
         * b. Example: Nancy M. Goldberg
         * <p/>
         * <p/>
         * 5. CapitalizedWord CapitalLetter @PersonVerbs
         * a. @PersonVerbs is a list of common verbs that are strongly associated with
         * people such as {said, met, walked, etc.}
         */

        public Chunker createPersonChunker() {
                GraphRegExp.Matcher Token = match("Token");
                GraphRegExp.Matcher Honorific = GraphUtils.regexp("^(Dr|Prof|Ms|Mrs|Mr)$", Token);
                GraphRegExp.Matcher CapitalizedWord = GraphUtils.regexp("^[A-Z]\\w+$", Token);
                GraphRegExp.Matcher PersonSuffix = GraphUtils.regexp("^(Jr|Sr)$", Token);
                GraphRegExp.Matcher CapitalLetter = GraphUtils.regexp("^[A-Z]$", Token);
                GraphRegExp.Matcher FirstName = GraphUtils.regexp("^(Bill|John)$", Token);
                GraphRegExp.Matcher Dot = match("Dot");
                GraphRegExp.Matcher Comma = match("Comma");
                GraphRegExp.Matcher PersonVerbs = GraphUtils.regexp("^(said|met|walked)$", Token);


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
                                        seq(mark("Person", seq(CapitalizedWord, CapitalLetter)), PersonVerbs)
                                )
                        )
                );
                return chunker;
        }

        @Test
        public void personExample() {
                String text = " Mr. Ahuna Edwards is first person. " +
                        "Devid Kent, Sr. is second person. " +
                        "Devid M. Donald is just another person. " +
                        "Bill Smith is person with first name. Jonny K said: I'm happy.";
                Chunker personChunker = createPersonChunker();
                SortedSet<String> result = new TreeSet<String>();
                for (Chunk ch : personChunker.chunk(new TextWithChunks(text))) {
                        if (ch.type.equals("Person")) {
                                System.out.println("Person[" + ch.getContent() + "]");
                                result.add(ch.getContent());
                        }
                }
                assertEquals("Bill Smith | Devid Kent, Sr. | Devid M. Donald | Jonny K | Mr. Ahuna Edwards", Joiner.on(" | ").join(result));

        }

        /////////////////////////////////////

        @Test
        public void predicateUsageIpAddressDetection() {
                GraphRegExp.Matcher Token = match("Token");
                GraphRegExp.Matcher Number = GraphUtils.regexp("^\\d+$", Token);

                GraphRegExp.Matcher NotNumber = GraphUtils.regexp("^[^\\d]+$", Token);
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
                        ).setDebugString(true)
                );

                SortedSet<String> result = new TreeSet<String>();
                String text = "some text 123.255.888.555 some text " +
                        "255.255.1.1 some text 1.1.255.134 some text 1.1.1.1.1.1.1.1.1";
                for (Chunk ch : ipChunker.chunk(new TextWithChunks(text))) {
                        if (ch.type.equals("IpAddress")) {
                                System.out.println("IpAddress[" + ch.getContent() + "]");
                                result.add(ch.getContent());
                        }
                }
                assertEquals("1.1.255.134 | 255.255.1.1", Joiner.on(" | ").join(result));
        }

        @Test
        public void numberBetween0To255() {
                Pattern pat = Pattern.compile("([1-9]|[1-9][0-9]|1[0-9][0-9]|2([0-4][0-9]|5[0-5]))");
                assertTrue(pat.matcher("249").matches());
                assertTrue(pat.matcher("244").matches());
                assertTrue(pat.matcher("254").matches());
                assertFalse(pat.matcher("256").matches());
                assertFalse(pat.matcher("056").matches());
                assertTrue(pat.matcher("19").matches());
                assertTrue(pat.matcher("1").matches());
                assertTrue(pat.matcher("99").matches());
        }

        @Test
        public void testWightedRegexps() {
                String data = "How is Mr adam when Mr John Smith is better person now";
                TextWithChunks textWithChunks = new TextWithChunks(data);
                GraphRegExp.Matcher upper = group("upper", GraphUtils.regexp("^[A-Z]", match("Token")));
                GraphRegExp.Matcher mr = GraphUtils.regexp("^Mr$", match("Token"));
                GraphRegExp.Matcher token = group("lower", match("Token"));
                Transformer<GraphMatchWrapper, Double> length = new Transformer<GraphMatchWrapper, Double>() {
                        public Double transform(GraphMatchWrapper m) {
                                return m.getChunksList().size() * 0.2;
                        }
                };
                GraphRegExpExt.WeightedMatcher tokens = weighted(star(token).reluctant(), length).setShowBestFist(true);
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

        }

        /*
       USA Addresses
        */
        public Chunker createAdressChunker() {
                GraphRegExp.Matcher Token = match("Token");
                GraphRegExp.Matcher Country = GraphUtils.regexp("^USA$", Token);
                GraphRegExp.Matcher Number = GraphUtils.regexp("^\\d+$", Token);
                GraphRegExp.Matcher StateLike = GraphUtils.regexp("^([A-Z]{2})$", Token);
                GraphRegExp.Matcher Postoffice = seq(match("BoxPrefix"), Number);
                GraphRegExp.Matcher Postcode =
                        mark("Postcode", seq(GraphUtils.regexp("^\\d{5}$", Token), opt(GraphUtils.regexp("^\\d{4}$", Token))));
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
                return chunker;
        }

        @Test
        public void testUSAAddresses() {
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
                TextWithChunks textWithChunks = new TextWithChunks(data);
                Chunker pipeline = createAdressChunker();
                Chunkers.execute(pipeline, textWithChunks);
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
        }

        /////////////
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

}
