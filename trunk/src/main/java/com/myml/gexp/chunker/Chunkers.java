package com.myml.gexp.chunker;


import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Author: java developer 1
 * Date: 04.02.2011
 * Time: 15:40:32
 */
public class Chunkers {
    public static Chunker pipeline(final Chunker... chunker) {
        return new Chunker() {
            public Collection<Chunk> chunk(TextWithChunks chunkText) {
                Collection<Chunk> result = newSet();
                TextWithChunks tempChunkText = new TextWithChunks(chunkText);
                for (Chunker ch : chunker) {
                    Collection<Chunk> chunks = ch.chunk(tempChunkText);
                    result.addAll(chunks);
                    tempChunkText.addAll(chunks);
                }
                return result;
            }
        };
    }

    public static Chunker regexp(final String type, final String regExp, final int group) {
        return new Chunker() {
            Pattern pat = Pattern.compile(regExp);

            public Collection<Chunk> chunk(TextWithChunks chunkText) {
                Collection<Chunk> result = newSet();
                Matcher matcher = pat.matcher(chunkText.text);
                while (matcher.find()) {
                    result.add(new Chunk(chunkText, type, matcher.start(group), matcher.end(group)));
                }
                return result;
            }
        };
    }

    public static Chunker regexp(final String type, final String regExp) {
        return regexp(type, regExp, 0);
    }

    public static void execute(Chunker chunker, TextWithChunks textWithChunks) {
        textWithChunks.addAll(chunker.chunk(textWithChunks));
    }

    //

    public static SortedSet<Chunk> newSet() {
        return new TreeSet<Chunk>();
    }

    @Deprecated
    public static String toChunksString(TextWithChunks text, int contextOffset, boolean withToString,
                                        String... showOnly) {
        StringBuilder sb = new StringBuilder();
        Collection<Chunk> set;
        if (showOnly.length == 0) {
            set = text;
        } else {
            set = text.retrieve(showOnly);
        }
        for (Chunk an : set) {
            if (withToString) {
                sb.append(String.format("%s[%s]//%s\n%s\n", an.type, replaceEol(an.getContent()), an.toString(),
                        replaceEol(StringUtils.substring(text.getContent(), an.start - contextOffset,
                                an.end + contextOffset))));
            } else {
                sb.append(String.format("%s[%s]\n%s\n", an.type, replaceEol(an.getContent()),
                        replaceEol(StringUtils.substring(text.getContent(), an.start - contextOffset,
                                an.start + contextOffset))));

            }
        }
        return sb.toString();
    }

    public static String toChunksStringEx(TextWithChunks text, int contextOffset, boolean withToString,
                                          String... showOnly) {
        StringBuilder sb = new StringBuilder();
        Collection<Chunk> set;
        if (showOnly.length == 0) {
            set = text;
        } else {
            set = text.retrieve(showOnly);
        }
        for (Chunk an : set) {
            String s = replaceEol(
                    StringUtils.substring(text.getContent(), Math.max(an.start - contextOffset,0), an.start)
                            + "[[[" + an.getContent() + "]]]"
                            + StringUtils.substring(text.getContent(), an.end, an.end + contextOffset)

            );
            if (withToString) {
                sb.append(String.format("%s[%s]//%s\n%s\n", an.type, replaceEol(an.getContent()), an.toString(), s));
            } else {
                sb.append(String.format("%s[%s]\n%s\n", an.type, replaceEol(an.getContent()), s));

            }
        }
        return sb.toString();
    }

    public static String replaceEol(String str) {
        str = str.replaceAll("\n", "\\\\n");
        str = str.replaceAll("\r", "\\\\r");
        return str;
    }
}
