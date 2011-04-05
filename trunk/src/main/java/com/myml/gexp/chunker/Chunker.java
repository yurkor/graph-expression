package com.myml.gexp.chunker;

import java.util.Collection;

/**
 * Author: java developer 1
 * Date: 04.02.2011
 * Time: 15:39:42
 */
public interface Chunker {
    Collection<Chunk> chunk(TextWithChunks chunkText);
}
