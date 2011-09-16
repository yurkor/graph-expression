package com.myml.gexp.chunker.common;

import com.myml.gexp.chunker.Chunker;
import com.myml.gexp.chunker.Chunkers;
import com.myml.gexp.chunker.TextWithChunks;
import org.junit.Assert;
import org.junit.Test;

//import static com.myml.gexp.chunker.common.GraphExpChunker.*;
import static com.myml.gexp.chunker.common.typedef.GraphUtils.*;

public class GraphExpChunkerTest extends Assert {

    //no need to run predefined pipeline when we use regexp(String regexp)
    @Test
    public void regexpPreporcessorTest() {
        TextWithChunks chunkText = new TextWithChunks("a a c b b c a a");
        GraphExpChunker ann = new GraphExpChunker("result",
                seq(matchRegexp("a"), matchRegexp("c"), matchRegexp("b"))
                , "b", "a", "c"
        );
        Chunkers.execute(ann, chunkText);


        assertEquals("result[a c b]\n" +
                "a [[[a c b]]] b c a a\n", Chunkers.toChunksStringEx(chunkText, 20, false,
                "result"));

    }
    //select all c without b ahead
    @Test
    public void notTest() {
        TextWithChunks chunkText = new TextWithChunks("a a c b b c a a");
        Chunker ch = Chunkers.pipeline(
                Chunkers.regexp("a", "a", 0),
                Chunkers.regexp("b", "b", 0),
                Chunkers.regexp("c", "c", 0)
        );
        Chunkers.execute(ch, chunkText);
        GraphExpChunker ann = new GraphExpChunker("result",
                seq(match("c"), not(match("b")))
                , "b", "a", "c"
        );
        Chunkers.execute(ann, chunkText);


        assertEquals("result[c]\n" +
                "a a c b b [[[c]]] a a\n", Chunkers.toChunksStringEx(chunkText, 20, false,
                "result"));

    }

    //select c where a exist tokens before
    @Test
    public void lookbehindTest() {
        TextWithChunks chunkText = new TextWithChunks("a a c b b c b a c c c c");
        Chunker ch = Chunkers.pipeline(
                Chunkers.regexp("a", "a", 0),
                Chunkers.regexp("b", "b", 0),
                Chunkers.regexp("c", "c", 0)
        );
        Chunkers.execute(ch, chunkText);
        GraphExpChunker ann = new GraphExpChunker("result",
                seq(lookbehind(match("a"), 2), match("c"))
                , "b", "a", "c"
        );
        Chunkers.execute(ann, chunkText);


        assertEquals("result[c]\n" +
                "a a [[[c]]] b b c b a c c c c\n" +
                "result[c]\n" +
                "a a c b b c b a [[[c]]] c c c\n", Chunkers.toChunksStringEx(chunkText, 20, false,
                "result"));

    }

    @Test
    public void lookaheadTest() {
        TextWithChunks chunkText = new TextWithChunks("a a c b b c b a c c c c");
        Chunker ch = Chunkers.pipeline(
                Chunkers.regexp("a", "a", 0),
                Chunkers.regexp("b", "b", 0),
                Chunkers.regexp("c", "c", 0)
        );
        Chunkers.execute(ch, chunkText);
        GraphExpChunker ann = new GraphExpChunker("result",
                seq(match("c"), lookahead(match("b")))
                , "a", "b", "c"

        );
        Chunkers.execute(ann, chunkText);


        assertEquals("result[c]\n" +
                "a a [[[c]]] b b c b a c c \n" +
                "result[c]\n" +
                "a a c b b [[[c]]] b a c c c c\n", Chunkers.toChunksStringEx(chunkText, 15, false,
                "result"));

    }

    //annotate all "b" + "a" (with AB) sequnces in tag "container" (with GoodContainer)
    // all sub matcher will be annotated
    @Test
    public void insideFindAllTest() {
        TextWithChunks chunkText = new TextWithChunks("START b a b a END START a b END a b a b a b a ");
        Chunker ch = Chunkers.pipeline(
                Chunkers.regexp("a", "a", 0),
                Chunkers.regexp("b", "b", 0),
                Chunkers.regexp("container", "START.*?END", 0)
        );
        Chunkers.execute(ch, chunkText);
        GraphExpChunker ann = new GraphExpChunker(null,
                insideFindAll(mark("GoodContainer", match("container")), mark("SubMatch", seq(match("b"), match("a"))))
        );
        Chunkers.execute(ann, chunkText);


        assertEquals("GoodContainer[START b a b a END]\n" +
                "\n" +
                "SubMatch[b a]\n" +
                "\n" +
                "SubMatch[b a]\n" +
                "\n", Chunkers.toChunksString(chunkText, 20, false,
                "GoodContainer", "SubMatch"));


    }

    //   only first  will be annotated
    @Test
    public void insideFindTest() {
        TextWithChunks chunkText = new TextWithChunks("START b a b a END START a b END a b a b a b a ");
        Chunker ch = Chunkers.pipeline(
                Chunkers.regexp("a", "a", 0),
                Chunkers.regexp("b", "b", 0),
                Chunkers.regexp("container", "START.*?END", 0)
        );
        Chunkers.execute(ch, chunkText);
        GraphExpChunker ann = new GraphExpChunker(null,
                insideFind(mark("GoodContainer", match("container")), mark("SubMatch", seq(match("b"), match("a"))))
        );
        Chunkers.execute(ann, chunkText);
        assertEquals("GoodContainer[START b a b a END]\n" +
                "\n" +
                "SubMatch[b a]\n" +
                "\n", Chunkers.toChunksString(chunkText, 20, false,
                "GoodContainer", "SubMatch"));
    }

    //select c
    @Test
    public void plusInPus() {
        TextWithChunks chunkText = new TextWithChunks("a a a b");
        Chunker ch = Chunkers.pipeline(
                Chunkers.regexp("a", "a", 0),
                Chunkers.regexp("b", "b", 0)
        );
        Chunkers.execute(ch, chunkText);
        System.out.println(Chunkers.toChunksString(chunkText, 20, false));

        GraphExpChunker ann = new GraphExpChunker("match",
                plus(
                        mark("element", plus(match("a")).setName("plus-2"))
                ).setName("plus-1")
        ).setDebugString(true).setMatchAll(false);
        Chunkers.execute(ann, chunkText);
        assertEquals("element[a a a]\n" +
                "a a a b\n" +
                "match[a a a]\n" +
                "a a a b\n", Chunkers.toChunksString(chunkText, 20, false, "match", "element"));
    }

    @Test
    public void simpleTest() {
        TextWithChunks chunkText = new TextWithChunks("a a c a k c b a c t a c ");
        Chunker ch = Chunkers.pipeline(
                Chunkers.regexp("a", "a", 0),
                Chunkers.regexp("c", "c", 0),
                Chunkers.regexp("OTHER", "\\w", 0)
        );
        Chunkers.execute(ch, chunkText);

        GraphExpChunker ann = new GraphExpChunker("match", seq(match("a"), match(), match("c"))
                , "OTHER").setMatchAll(false);
        Chunkers.execute(ann, chunkText);
        assertEquals("match[a a c]\n" +
                "c a k c b a c t \n" +
                "match[a k c]\n" +
                "c b a c t a c \n", Chunkers.toChunksString(chunkText, 20, false, "match"));
    }

}