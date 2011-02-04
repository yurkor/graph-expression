package com.myml.gexp.chunker;

import java.util.Collection;

/**
 * Author: Yura Korolov
 * Date: 04.02.2011
 * Time: 15:39:42
 */
public interface Chunker {
    Collection<Chunk> chunk(TextWithChunks chunkText);
}
