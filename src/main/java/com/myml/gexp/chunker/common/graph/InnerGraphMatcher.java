package com.myml.gexp.chunker.common.graph;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.myml.gexp.chunker.common.GraphExpChunker;
import com.myml.gexp.chunker.common.GraphMatchWrapper;
import com.myml.gexp.graph.matcher.GraphRegExp;
import com.myml.gexp.graph.matcher.GraphRegExp.*;
import org.apache.commons.collections15.Predicate;

import java.util.List;

/**
 * Author: java developer 1
 * Date: 12.03.11
 * Time: 14:05
 */
public class InnerGraphMatcher extends Matcher {
        Matcher condition;
        Matcher matcherInCondtion;
        boolean returnContainerMatch = false;

        public InnerGraphMatcher(Matcher condition, Matcher matcherInCondtion) {
                this.condition = condition;
                this.matcherInCondtion = matcherInCondtion;
        }

        public InnerGraphMatcher setPerformFullMatch() {
                this.matcherInCondtion = GraphExpChunker.matchChunk(matcherInCondtion, new Predicate<GraphMatchWrapper>() {
                        @Override
                        public boolean evaluate(GraphMatchWrapper match2) {
                                Node endNodeOfMatch2 = match2.match.getEndNode();
                                Node endNode = match2.getContext().get(InsideContext.END_NODE);
                                return (endNode == endNodeOfMatch2 || Iterables.isEmpty(match2.getContext().getEdges(endNodeOfMatch2)));
                        }
                });
                return this;
        }


        public InnerGraphMatcher setReturnContainerMatch(boolean returnContainerMatch) {
                this.returnContainerMatch = returnContainerMatch;
                return this;
        }

        public static class InsideContext implements GraphRegExp.GraphContext {
                GraphRegExp.GraphContext delegate;
                GraphExpChunker.PositionNode endNode;
                public static final String END_NODE = "END_NODE";

                public InsideContext(GraphRegExp.GraphContext delegate, GraphExpChunker.PositionNode endNode) {
                        this.delegate = delegate;
                        this.endNode = endNode;
                }

                public Iterable<Edge> getEdges(Node node) {
                        return Iterables.filter(delegate.getEdges(node), new com.google.common.base.Predicate<Edge>() {
                                public boolean apply(Edge input) {
                                        return ((GraphExpChunker.PositionNode) delegate.getEndNode(input)).getPosition() <= endNode.getPosition();
                                }
                        });
                }

                public Node getEndNode(Edge edge) {
                        return delegate.getEndNode(edge);
                }

                public <T> T put(String key, T value) {
                        return null;
                }

                public <T> T get(String key) {
                        return END_NODE.equals(key) ? (T) endNode : delegate.<T>get(key);
                }
        }

        @Override
        public MatchResult find(final Node node, final GraphContext context) {
                return new MatchResult() {
                        final MatchResult head = condition.find(node, context);
                        MatchResult tail = null;
                        private GraphExpChunker.PositionNode endNode;

                        @Override
                        protected Match nextInner() {
                                if (tail != null && tail.next() != null) {
                                        if (returnContainerMatch) {
                                                return new JoinMatch(head.current(), tail.current());
                                        } else {
                                                return new JoinMatch(tail.current(), head.current()) {
                                                        public Node getEndNode() {
                                                                return endNode;
                                                        }

                                                };
                                        }
                                } else {
                                        if (head.next() != null) {
                                                endNode = (GraphExpChunker.PositionNode) head.current().getEndNode();
                                                tail = matcherInCondtion.find(node, new InsideContext(context, endNode));
                                                return nextInner();
                                        } else return null;
                                }
                        }
                };
        }


        @Override
        public List<Matcher> getSubMatchers() {
                return Lists.newArrayList(condition, matcherInCondtion);
        }
}
