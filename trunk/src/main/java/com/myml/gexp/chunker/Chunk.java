package com.myml.gexp.chunker;

/**
 * Author: Yura Korolov
 * Date: 04.02.2011
 * Time: 15:32:55
 */
public class Chunk implements Comparable<Chunk> {
    public final TextWithChunks text;
    public final String type;
    public final int start;
    public final int end;

    public Chunk(TextWithChunks text, String type, int start, int end) {
        this.text = text;
        this.type = type;
        this.start = start;
        this.end = end;
    }

    public int compareTo(Chunk o) {
        int res = start - o.start;
        if (res != 0) return res;
        res = end - o.end;
        if (res != 0) return res;
        res = type.compareTo(o.type);
        if (res != 0) return res;
        //TODO: is this good?
        res = System.identityHashCode(this) - System.identityHashCode(o);
        return res;
    }

    public String getContent() {
        return text.getContent().substring(start, end);
    }
}
