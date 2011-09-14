package com.myml.gexp.graph.matcher;

import com.google.common.collect.Lists;
import org.apache.commons.collections15.Predicate;

import java.util.*;

/**
 * Author: java developer 1
 * Date: 07.10.2010
 * Time: 11:59:51
 */
public class GraphRegExp {


    public static interface Node {
    }

    public static interface Edge {
    }

    //List<List<Edge>>

    public static abstract class MatchResult {
        private Match current;

        protected abstract Match nextInner();

        public final Match next() {
            current = nextInner();
            return current;
        }

        public Match current() {
            return current;
        }

        public List<List<Edge>> getAll() {
            Match m = null;
            List<List<Edge>> edges = Lists.newArrayList();
            while ((m = next()) != null) {
                edges.add(m.getList());
            }
            return edges;
        }

        public List<Match> getAllMatches() {
            Match m = null;
            List<Match> edges = Lists.newArrayList();
            while ((m = next()) != null) {
                edges.add(m);
            }
            return edges;
        }

        public boolean isBestFirst() {
            return false;
        }
    }

    public static abstract class Match {

        public abstract List<Edge> getList();

        public abstract List<Match> getSubMatches();

        public abstract Node getEndNode();

        public abstract Node getStartNode();

        public abstract GraphContext getContext();
    }

    public static class CompositeMatch extends Match {
        List<Match> matches;
        private GraphContext context;

        public CompositeMatch(GraphContext context, Match... matches) {
            this.matches = Arrays.asList(matches);
            this.context = context;
        }

        public List<Edge> getList() {
            List<Edge> list = Lists.newArrayList();
            for (Match m : matches) {
                list.addAll(m.getList());
            }
            return list;
        }

        public List<Match> getSubMatches() {
            return matches;
        }

        public Node getEndNode() {
            for (int i = matches.size() - 1; i >= 0; --i) {
                Match m = matches.get(i);
                Node endNode = m.getEndNode();
                if (endNode != null) return endNode;
            }
            return null;
        }

        @Override
        public Node getStartNode() {
            for (Match m : matches) {
                Node endNode = m.getStartNode();
                if (endNode != null) return endNode;
            }
            return null;
        }

        @Override
        public GraphContext getContext() {
            return context;
        }

        @Override
        public String toString() {
            return "CM{" +
                    matches +
                    '}';
        }
    }

    public static class LeafMatch extends Match {
        List<Edge> list;
        Node startNode;
        Node endNode;
        GraphContext context;

        public LeafMatch(GraphContext context, Node startNode, Node endNode, List<Edge> list) {
//            Assert.notNull(startNode);
            this.startNode = startNode;
            this.endNode = endNode;
            this.list = list;
            this.context = context;
        }

        public List<Edge> getList() {
            return list;
        }

        public List<Match> getSubMatches() {
            return Collections.emptyList();
        }

        @Override
        public Node getEndNode() {
            return endNode;
        }

        @Override
        public Node getStartNode() {
            return startNode;
        }

        @Override
        public GraphContext getContext() {
            return context;
        }

        @Override
        public String toString() {
            return "LM{" + list + "}";
        }
    }

    public static class JoinMatch extends Match {
        Match mainMatch;
        Match joinMatch;

        public JoinMatch(Match mainMatch, Match joinMatch) {
            this.mainMatch = mainMatch;
            this.joinMatch = joinMatch;
        }

        @Override
        public List<Edge> getList() {
            return mainMatch.getList();
        }

        @Override
        public List<Match> getSubMatches() {
            return Lists.newArrayList(mainMatch, joinMatch);
        }

        @Override
        public Node getEndNode() {
            return mainMatch.getEndNode();
        }

        @Override
        public Node getStartNode() {
            return mainMatch.getStartNode();
        }

        @Override
        public GraphContext getContext() {
            return mainMatch.getContext();
        }
    }

    public interface GraphContext {
        public Iterable<Edge> getEdges(Node node);

        public Node getEndNode(Edge edge);

        public <T> T put(String key, T value);

        public <T> T get(String key);

    }

    public static abstract class Matcher {
        protected String name;

        public abstract MatchResult find(final Node node, GraphContext context);

        public abstract List<Matcher> getSubMatchers();

        public Matcher setName(String name) {
            this.name = name;
            return this;
        }
    }


    public static List<List<Edge>> join(List<Edge> start, List<List<Edge>> ends) {
        List<List<Edge>> result = Lists.newArrayList();
        for (List<Edge> edges : ends) {
            List<Edge> currentList = Lists.newArrayList(start);
            currentList.addAll(edges);
            result.add(currentList);
        }
        return result;
    }

    public static class PredicateMatcher extends Matcher {
        Predicate<Edge> matchP;

        public PredicateMatcher(Predicate<? extends Edge> matchP) {
            this.matchP = (Predicate<Edge>) matchP;
        }

        public MatchResult find(final Node node, final GraphContext context) {
            return new MatchResult() {
                Iterator<Edge> edgeIt = context.getEdges(node).iterator();

                public Match nextInner() {
                    while (edgeIt.hasNext()) {
                        Edge cur = edgeIt.next();
                        if (matchP.evaluate(cur)) {
                            return new LeafMatch(context, node, context.getEndNode(cur),
                                    Collections.singletonList(cur));
                        }
                    }
                    return null;
                }
            };
        }

        public List<Matcher> getSubMatchers() {
            return Collections.emptyList();
        }
    }

    public static class GroupPredicateMatcher extends Matcher {
        Predicate<Match> matchP;
        Matcher matcher;

        public GroupPredicateMatcher(Predicate<? extends Match> matchP, Matcher matcher) {
            this.matchP = (Predicate<Match>) matchP;
            this.matcher = matcher;
        }

        public MatchResult find(final Node node, final GraphContext context) {
            return new MatchResult() {
                MatchResult edgeIt = matcher.find(node, context);

                public Match nextInner() {
                    while (edgeIt.next() != null) {
                        if (matchP.evaluate(edgeIt.current())) {
                            return edgeIt.current();
                        }
                    }
                    return null;
                }
            };
        }

        public List<Matcher> getSubMatchers() {
            return Collections.singletonList(matcher);
        }
    }


    public static class LookaheadMatcher extends Matcher {
        private Matcher matcher;
        private Predicate<Node> matcher2;
        private Collection<Matcher> inPredicate = Lists.newArrayList();

        public LookaheadMatcher setInPredicate(Collection<Matcher> inPredicate) {
            this.inPredicate = inPredicate;
            return this;
        }

        public LookaheadMatcher(Matcher matcher, Predicate<Node> matcher2) {
            this.matcher = matcher;
            this.matcher2 = matcher2;
        }

        @Override
        public MatchResult find(Node node, GraphContext context) {
            if (!matcher2.evaluate(node)) {
                return new MatchResult() {
                    protected Match nextInner() {
                        return null;
                    }
                };
            } else {
                return matcher.find(node, context);
            }
        }

        @Override
        public List<Matcher> getSubMatchers() {
            List<Matcher> mm = Lists.newArrayList(inPredicate);
            mm.add(matcher);
            return mm;
        }
    }

    protected static Node getNextNode(Node current, Match m) {
        Node n = m.getEndNode();
        return (n == null) ? current : n;
    }

    public static class CutMatcher extends Matcher {
        private Matcher matcher;

        public CutMatcher(Matcher matcher) {
            this.matcher = matcher;
        }

        @Override
        public MatchResult find(final Node node, final GraphContext context) {
            return new MatchResult() {
                boolean first = true;
                MatchResult matchResult = matcher.find(node, context);

                @Override
                protected Match nextInner() {
                    if (first) {
                        first = false;
                        return matchResult.next();
                    }
                    return null;
                }
            };
        }

        @Override
        public List<Matcher> getSubMatchers() {
            return Collections.singletonList(matcher);
        }
    }

    public static class Seq extends Matcher {
        List<Matcher> matchers;

        public Seq(List<Matcher> matchers) {
            this.matchers = matchers;
        }

        public MatchResult find(final Node node, final GraphContext context) {
            return new MatchResult() {
                int index = 0;
                List<MatchResult> results = new ArrayList<MatchResult>(matchers.size());
                List<Match> resultMatch = new ArrayList<Match>(matchers.size());

                public Match nextInner() {
                    while (index >= 0) {
                        if (results.size() - 1 < index) {
                            results.add(matchers.get(index).find(getLastNode(), context));
                        }
                        if (results.get(index).next() == null) {
                            index--;
                            continue;
                        } else {
                            if (resultMatch.size() - 1 < index) {
                                resultMatch.add(results.get(index).current());
                            } else {
                                resultMatch.set(index, results.get(index).current());
                            }
                            if (index == matchers.size() - 1) {
                                return new CompositeMatch(context, resultMatch.toArray(new Match[resultMatch.size()]));
                            } else {
                                trunkList(results, index + 1);
                                trunkList(resultMatch, index + 1);
                                index++;
                            }
                            continue;
                        }

                    }
                    return null;
                }

                <T> void trunkList(List<T> list, int size) {
                    while (list.size() > size) {
                        list.remove(list.size() - 1);
                    }
                }

                Node getLastNode() {
                    for (int i = resultMatch.size() - 1; i >= 0; i--) {
                        Match m = resultMatch.get(i);
                        Node node1 = m.getEndNode();
                        if (node1 != null) return node1;
                    }
                    return node;
                }
            };
        }

        public List<Matcher> getSubMatchers() {
            return matchers;
        }
    }
    /*
   star(i,j)(a) =seq(a)... + star(0,k)
    star(0,k)=or(seq(a, star(0,k-1)), empty())
    star(0,1)=or(a, empty())
    */

    public static class Star extends Matcher {
        public Star(Matcher matcher, int minCount, int maxCount) {
            this.innerMatcher = matcher;
            this.minCount = minCount;
            this.maxCount = maxCount;
//            Utils.assertArg("minCount > maxCount", minCount <= maxCount);
        }

        public Star setGreedy(boolean greedy) {
            this.greedy = greedy;
            return this;
        }

        public Star reluctant() {
            this.greedy = false;
            return this;
        }

        public Star greedy() {
            this.greedy = true;
            return this;
        }

        Matcher innerMatcher;
        int minCount;
        int maxCount;
        boolean greedy = true;

        public MatchResult find(final Node node, final GraphContext context) {
            return new MatchResult() {
                class State {
                    Node start;
                    MatchResult res;
                    int min;
                    int max;

                    boolean head() {
                        return (min <= 1 && max > 0);
                    }

                    boolean tail() {
                        return (max > 1);
                    }

                    State(Node start, int min, int max, MatchResult res) {
                        this.start = start;
                        this.min = min;
                        this.max = max;
                        this.res = res;
                    }

                    public Match next() {
                        result = res.next();
                        return result;
                    }

                    Match result;

                    private Match empty = new CompositeMatch(context);

                    public Match returnNull() {
                        if (min == 0 && empty != null) {
                            Match res = empty;
                            empty = null;
                            return res;
                        }
                        return null;
                    }

                    public Node getEndNode() {
                        Node res = result.getEndNode();
                        return res != null ? res : start;
                    }
                }

                List<State> states = new ArrayList<State>();

                {
                    states.add(new State(node, minCount, maxCount, innerMatcher.find(node, context)));
                }

                Match returnMatch() {
                    Match[] res = new Match[states.size()];
                    for (int i = states.size() - 1; i >= 0; i--) {
                        res[i] = states.get(i).result;
                    }
                    return new CompositeMatch(context, res);
                }

                @Override
                protected Match nextInner() {
                    if (greedy) {
                        return greedyNextInner();
                    } else {
                        return notGreedyNextInner();
                    }
                }

                private int notZero(int i) {
                    return Math.max(i, 1);
                }

                private Match greedyNextInner() {
                    while (!states.isEmpty()) {
                        State s = states.get(states.size() - 1);
                        if (s.result != null && s.head()) {
                            Match m = returnMatch();
                            s.result = null;
                            return m;
                        }
                        Match m = s.next();
                        if (m == null) {
                            m = s.returnNull();
                            if (m != null) return m;

                            states.remove(states.size() - 1);
                            continue;
                        }
                        if (s.tail()) {
                            Node n = s.getEndNode();
                            states.add(new State(n, notZero(s.min - 1), notZero(s.max - 1), innerMatcher.find(n,
                                    context)));
                            continue;
                        }

                    }
                    return null;
                }

                private Match notGreedyNextInner() {
                    while (!states.isEmpty()) {
                        State s = states.get(states.size() - 1);
                        Match m = s.returnNull();
                        if (m != null) return m;
                        m = s.next();
                        if (m != null) {
                            if (s.head()) m = returnMatch();
                            if (s.tail()) {
                                Node n = s.getEndNode();
                                states.add(new State(n, notZero(s.min - 1), s.max - 1, innerMatcher.find(n, context)));
                            }
                            if (s.head()) return m;
                            continue;
                        }
                        states.remove(states.size() - 1);
                    }
                    return null;
                }
            };
        }

        @Override
        public List<Matcher> getSubMatchers() {
            return Collections.singletonList(innerMatcher);
        }
    }

    public static class Or extends Matcher {
        List<Matcher> matchers;

        public Or(List<Matcher> matchers) {
            this.matchers = matchers;
        }

        @Override
        public MatchResult find(final Node node, final GraphContext context) {
            final List<MatchResult> results = Lists.newArrayList();
            for (Matcher matcher : matchers) {
                results.add(matcher.find(node, context));
            }
            return new MatchResult() {
                int cur = 0;
                MatchResult curR = matchers.get(cur).find(node, context);

                @Override
                protected Match nextInner() {
                    if (cur > matchers.size() - 1) return null;
                    Match m = null;
                    while (curR != null && (m = curR.next()) == null) {
                        cur++;
                        if (cur < matchers.size()) curR = matchers.get(cur).find(node, context);
                        else curR = null;
                    }
                    return m;
                }
            };
        }

        @Override
        public List<Matcher> getSubMatchers() {
            return matchers;
        }
    }
}
