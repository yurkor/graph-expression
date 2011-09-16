package com.myml.gexp.chunker.common;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.myml.gexp.chunker.Chunk;
import com.myml.gexp.chunker.Chunkers;
import com.myml.gexp.graph.matcher.GraphRegExp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: java developer 1
 * Date: 04.02.2011
 * Time: 16:13:13
 */
public class GraphMatchWrapper {
        public final GraphRegExp.Match match;
        Map<String, GraphMatchWrapper> map;

        public GraphMatchWrapper(GraphRegExp.Match match) {
                this.match = match;
        }

        public GraphMatchWrapper get(String groupName) {
                init();
                return map.get(groupName);
        }

        public List<GraphMatchWrapper> getList(String groupName) {
                List<GraphMatchWrapper> result = Lists.newArrayList();
                for (GraphExpChunker.MarkedMatch mm : GraphExpChunker.getAllMarked(match)) {
                        if (groupName.equals(mm.markMatcher.groupName)) result.add(new GraphMatchWrapper(mm));
                }
                return result;
        }

        private void init() {
                if (map == null) {
                        map = new HashMap<String, GraphMatchWrapper>(10);
                        for (GraphExpChunker.MarkedMatch mm : GraphExpChunker.getAllMarked(match)) {
                                map.put(mm.markMatcher.groupName, new GraphMatchWrapper(mm));
                        }
                }
        }

        public List<Chunk> getChunksList() {
                return GraphExpChunker.toList(match.getList());
        }

        static GraphRegExp.Edge getFirstEdge(GraphRegExp.Match mm, boolean isFirst) {
                if (mm.getSubMatches().isEmpty()) {
                        List<GraphRegExp.Edge> list = mm.getList();
                        if (list.isEmpty()) return null;
                        return isFirst ? Iterables.get(list, 0) : Iterables.getLast(list);
                }
                final Iterable<GraphRegExp.Match> col;
                if (!isFirst) col = Iterables.reverse(mm.getSubMatches());
                else col = mm.getSubMatches();
                for (GraphRegExp.Match m : col) {
                        GraphRegExp.Edge edge = getFirstEdge(m, isFirst);
                        if (edge != null) return edge;
                }
                return null;
        }

        public String getText() {
                GraphRegExp.Edge first = getFirstEdge(match, true);
                GraphRegExp.Edge last = getFirstEdge(match, false);
                if (first != null && last != null) {
                        return ((GraphExpChunker.ChunkEdge) first).getAnn().text().text.substring(
                                ((GraphExpChunker.ChunkEdge) first).getAnn().start,
                                ((GraphExpChunker.ChunkEdge) last).getAnn().end
                        );
                } else {
                        return "";
                }
        }

        public String getGroupName() {
                if (match instanceof GraphExpChunker.MarkedMatch) {
                        return ((GraphExpChunker.MarkedMatch) match).markMatcher.groupName;
                } else {
                        return null;
                }
        }

        public Iterable<GraphMatchWrapper> getChildren() {
                return Iterables.transform(match.getSubMatches(), new Function<GraphRegExp.Match, GraphMatchWrapper>() {
                        public GraphMatchWrapper apply(GraphRegExp.Match from) {
                                return new GraphMatchWrapper(from);
                        }
                });
        }

        public GraphRegExp.GraphContext getContext() {
                return match.getContext();
        }

        @Override
        public String toString() {
                return "GraphMatchWrapper{" + Chunkers.replaceEol(getText()) + '}';
        }
}
