package com.myml.gexp.chunker.common;

import com.myml.gexp.chunker.Chunk;

/**
 * User: ykorolov
 * Date: 2/15/12
 * Time: 11:46 AM
 */
public interface GraphMatchAction {
    Chunk doAction(GraphMatchWrapper wrapper, Chunk chunk);
}
