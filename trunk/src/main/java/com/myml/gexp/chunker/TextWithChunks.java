package com.myml.gexp.chunker;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.Predicate;

import java.util.*;

/**
 * Author: java developer 1
 * Date: 04.02.2011
 * Time: 15:33:41
 */
public class TextWithChunks extends AbstractCollection<Chunk> {
    public final String text;
    private SortedSet<Chunk> chunks = Chunkers.newSet();

    public TextWithChunks(String text) {
        this.text = text;
    }

    public TextWithChunks(TextWithChunks chunkText) {
        text = chunkText.text;
        chunks.addAll(chunkText.chunks);
    }

    public Collection<Chunk> retrieve(Predicate<? super Chunk> chunkPredicate) {
        return CollectionUtils.select(chunks, chunkPredicate);
    }

    public SortedSet<Chunk> retrieve(final String... types) {
        final Set<String> set = Sets.newHashSet(types);
        return CollectionUtils.select(chunks, new Predicate<Chunk>() {
            public boolean evaluate(Chunk chunk) {
                return set.contains(chunk.type);
            }
        }, Chunkers.newSet());
    }

    public Iterator<Chunk> iterator() {
        return chunks.iterator();
    }

    public int size() {
        return chunks.size();
    }

    public boolean add(Chunk chunk) {
        Preconditions.checkArgument(chunk.text.text.hashCode() == this.text.hashCode(), "texts are different");
        chunk.text = this;
        return chunks.add(chunk);
    }

    public SortedSet<Chunk> retrieve(final Set<String> types) {
        return CollectionUtils.select(chunks, new Predicate<Chunk>() {
            public boolean evaluate(Chunk chunk) {
                return types.contains(chunk.type);
            }
        }, Chunkers.newSet());
    }

    public String getContent() {
        return text;
    }

    public SortedSet<Chunk> getChunks() {
        return chunks;
    }
}
