package com.myml.gexp.chunker.common.graph;

import com.myml.gexp.chunker.common.GraphExpChunker;
import com.myml.gexp.graph.matcher.GraphRegExp;

import java.util.Collections;
import java.util.List;

/**
 * Author: java developer 1
 * Date: 12.03.11
 * Time: 14:08
 */
public class NextNodeMatcher extends GraphRegExp.Matcher {

    boolean inverse = false;

    public NextNodeMatcher setInverse(boolean inverse) {
        this.inverse = inverse;
        return this;
    }

    @Override
    public GraphRegExp.MatchResult find(final GraphRegExp.Node node, final GraphRegExp.GraphContext context) {
        return new GraphRegExp.MatchResult() {
            //TODO: check for correct edge
            boolean first = true;
            GraphExpChunker.PositionNode node2;

            {
                node2 = ((GraphExpChunker.PositionNode) node);
                node2 = inverse? node2.getPrevNode(): node2.getNextNode();
            }

            @Override
            protected GraphRegExp.Match nextInner() {
                if(!first) return null;
                first = false;
                GraphExpChunker.PositionNode endNode = context.get(InnerGraphMatcher.InsideContext.END_NODE);
                //TODO: END_NODE context
                if(node2 != null && endNode != null && node2.getPosition() > endNode.getPosition()) {
                    node2 = null;
                }
                if (node2 != null) {
                    if(!inverse) {
                        return new GraphRegExp.LeafMatch(context, node,node2, Collections.<GraphRegExp.Edge>emptyList());
                    } else {
                        return new GraphRegExp.LeafMatch(context, node2,node2, Collections.<GraphRegExp.Edge>emptyList());
                    }
                }
                return null;
            }
        };
    }

    @Override
    public List<GraphRegExp.Matcher> getSubMatchers() {
        return Collections.emptyList();
    }
}
