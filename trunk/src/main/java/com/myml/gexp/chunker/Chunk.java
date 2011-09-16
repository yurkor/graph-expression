package com.myml.gexp.chunker;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

import java.util.Map;

import static com.google.common.base.Objects.equal;

/**
 * Author: java developer 1
 * Date: 04.02.2011
 * Time: 15:32:55
 */
public class Chunk implements Comparable<Chunk> {
    TextWithChunks text; //not final for rebind
    public final String type;
    public final int start;
    public final int end;
    private Map<String,Object> features;

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

    public Chunk set(String name, Object property) {
        features().put(name, property);
        return this;
    }
    public <T> T get(String name) {
        if(features == null) return null;
        else return (T)features().get(name);
    }

    public Map<String,Object> features() {
        if(features == null) features = Maps.newHashMap();
        return features;
    }

    public TextWithChunks text() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chunk chunk = (Chunk) o;
        return equal(end, chunk.end) && equal(start, chunk.start)
                && equal(type, chunk.type);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type, start, end);
    }
}
