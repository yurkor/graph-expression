package com.myml.gexp.chunker.common;

import com.myml.gexp.chunker.Chunker;
import com.myml.gexp.chunker.Chunkers;
import com.myml.gexp.chunker.TextWithChunks;
import org.apache.commons.collections15.Predicate;
import org.junit.Assert;
import org.junit.Test;

import static com.myml.gexp.chunker.common.GraphExpChunker.*;

public class GraphExpChunkerTest extends Assert {

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