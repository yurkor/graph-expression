package com.myml.gexp.examples;

import com.myml.gexp.chunker.Chunker;
import com.myml.gexp.chunker.Chunkers;
import com.myml.gexp.chunker.TextWithChunks;
import com.myml.gexp.chunker.common.GraphExpChunker;
import com.myml.gexp.chunker.common.GraphMatchWrapper;
import com.myml.gexp.chunker.common.typedef.GraphUtils;
import com.myml.gexp.graph.matcher.GraphRegExp;
import org.apache.commons.collections15.Predicate;
import org.junit.Assert;
import org.junit.Test;

import static com.myml.gexp.chunker.common.GraphExpChunker.match;
import static com.myml.gexp.chunker.common.typedef.GraphUtils.mark;
import static com.myml.gexp.chunker.common.typedef.GraphUtils.regexp;
import static com.myml.gexp.graph.matcher.GraphRegExpMatchers.*;
import static org.apache.commons.lang.StringUtils.countMatches;

/**
 * Author: java developer 1
 * Date: 15.03.11
 * Time: 14:17
 */
public class SentenceSplitterExampleTest extends Assert {
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

        @Test
        public void testDateTime() {
                String data = "A small letter after dot, for example something. means that the sentence is not finished.\n" +
                        "Sentence (can not be unbalanced by brackets. Because this part is also joined).\n" +
                        "Here many acronyms A. B. C. and then end.\n" +
                        "And there's no dot, but it's the last";
                TextWithChunks textWithChunks = new TextWithChunks(data);
                Chunker pipeline = createSentenceChunker();
                Chunkers.execute(pipeline, textWithChunks);
                assertEquals("Sentence[A small letter after dot, for example something. means that the sentence is not finished.]\n" +
                        "\n" +
                        "Stop[.]\n" +
                        "ed.\\n\n" +
                        "Sentence[Sentence (can not be unbalanced by brackets. Because this part is also joined).]\n" +
                        ".\\nSe\n" +
                        "Stop[.]\n" +
                        "ts. \n" +
                        "Stop[).]\n" +
                        "ed).\n" +
                        "Sentence[Here many acronyms A. B. C. and then end.]\n" +
                        ".\\nHe\n" +
                        "Stop[.]\n" +
                        "nd.\\n\n" +
                        "Sentence[And there's no dot, but it's the last]\n" +
                        ".\\nAn\n", Chunkers.toChunksString(textWithChunks, 2, false, "Sentence", "Stop"));
        }

}
