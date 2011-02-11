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
 * Author: Yura Korolov
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
}
