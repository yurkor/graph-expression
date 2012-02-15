package com.myml.gexp.chunker.common;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.myml.gexp.chunker.Chunk;
import com.myml.gexp.chunker.Chunker;
import com.myml.gexp.chunker.Chunkers;
import com.myml.gexp.chunker.TextWithChunks;
import com.myml.gexp.chunker.common.graph.InnerGraphMatcher;
import com.myml.gexp.chunker.common.graph.NextNodeMatcher;
import com.myml.gexp.graph.matcher.GraphRegExp;
import com.myml.gexp.graph.matcher.GraphRegExpExt;
import com.myml.gexp.graph.matcher.GraphRegExpMatchers;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.PredicateUtils;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.lang.StringUtils;

import java.util.*;

import static com.myml.gexp.graph.matcher.GraphRegExp.*;

/**
 * Author: java developer 1
 * Date: 04.02.2011
 * Time: 15:55:05
 */
public class GraphExpChunker extends GraphRegExpMatchers implements Chunker {
    public static final String END = "DOCUMENT.END";
    public static final String START = "DOCUMENT.START";

    private Matcher matcher;
    private String rootCategory;
    private Set<String> workOnCategories;
    private boolean matchFromAllPosition = false;
    private boolean matchAll = false;
    private boolean debugString = false;
    private Set<Chunker> preprocessors = Sets.newLinkedHashSet();

    GraphConverter converter = GraphConverter.WEAK_LINKS;
    public static final GraphContext CONTEXT = new GraphContext() {
        public Iterable<Edge> getEdges(Node node) {
            return (List) ((PositionNode) node).edges;
        }

        public Node getEndNode(Edge edge) {
            return ((ChunkEdge) edge).endNode;
        }

        public <T> T put(String key, T value) {
            throw new UnsupportedOperationException("Use context matcher wrapper to put some values to context");
        }

        public <T> T get(String key) {
            return null;
        }
    };

    public GraphExpChunker setConverter(GraphConverter converter) {
        this.converter = converter;
        return this;
    }

    public enum GraphConverter {
        GATE_COMPATIBLE {
            @Override
            public void process(PositionNode prevN, PositionNode curN) {
                boolean hasLink = false;
                for (Edge edge : CONTEXT.getEdges(prevN)) {
                    if (CONTEXT.getEndNode(edge) != prevN) {
                        hasLink = true;
                        break;
                    }
                }
                if (!hasLink) {
                    //add only first annotations
                    int smallest = 1000;
                    for (ChunkEdge e : curN.getAnnEdges()) {
                        smallest = Math.min(smallest, e.ann.start);
                    }
                    for (ChunkEdge e : curN.getAnnEdges()) {
                        if (smallest == e.ann.start) {
                            prevN.edges.add(e);
                        }
                    }
                }
            }
        },
        WEAK_LINKS {
            @Override
            public void process(PositionNode prevN, PositionNode curN) {
                boolean hasLink = false;
                for (Edge edge : CONTEXT.getEdges(prevN)) {
                    if (CONTEXT.getEndNode(edge) == curN) {
                        hasLink = true;
                        break;
                    }
                }
                if (!hasLink) {
                    Iterables.addAll(prevN.edges, (Iterable) CONTEXT.getEdges(curN));
                }
            }
        };

        public void process(PositionNode prevNode, PositionNode currentNode) {
        }

        public boolean desc() {
            return true;
        }

    }

    public GraphExpChunker setDebugString(boolean debugString) {
        this.debugString = debugString;
        return this;
    }

    public GraphExpChunker(String rootCategory, Matcher matcher, String... strs) {
        this.matcher = matcher;
        workOnCategories = Sets.newHashSet(strs);
        this.rootCategory = rootCategory;
        LinkedList<Matcher> heads = new LinkedList<Matcher>();
        heads.add(matcher);
        Set<Matcher> visited = Sets.newHashSet();
        while (!heads.isEmpty()) {
            Matcher m = heads.removeFirst();
            for (Matcher mm : m.getSubMatchers()) {
                if (!visited.contains(mm)) {
                    visited.add(mm);
                    heads.add(mm);
                }
            }
            if (m instanceof CategoriesHolder) {
                CategoriesHolder ch = (CategoriesHolder) m;
                workOnCategories.addAll(ch.getCategories());
                if (ch.isRegExps()) {
                    for (String s : ch.getCategories())
                        preprocessors.add(Chunkers.regexp(s, s));
                }
            }
        }
    }


    public GraphExpChunker setMatchFromAllPosition(boolean matchFromAllPosition) {
        this.matchFromAllPosition = matchFromAllPosition;
        return this;
    }

    public GraphExpChunker setMatchAll(boolean matchAll) {
        this.matchAll = matchAll;
        return this;
    }

    public List<Node> toNode(Collection<Chunk> annsSet) {
        class NodeMap {
            private TreeMap<Integer, Node> nodes = Maps.newTreeMap();

            private Node getNode(int pos) {
                Node n = nodes.get(pos);
                if (n == null) {
                    nodes.put(pos, n = new PositionNode(pos));
                }
                return n;
            }

            Node getRoot() {
                if (nodes.isEmpty()) return null;
                return nodes.get(nodes.firstKey());
            }

            List<Node> getList() {
                return Lists.newArrayList(nodes.values());
            }

            public void putChunk(Chunk ann) {
                ChunkEdge edge = new ChunkEdge(ann, getNode(ann.end));
                ((PositionNode) getNode(ann.start)).edges.add(edge);
            }

            private void process() {
                List<Node> nodesList = Lists.newArrayList(nodes.values());
                for (int i = 1; i < nodesList.size(); i++) {
                    ((PositionNode) (nodesList.get(i - 1))).setNextNode((PositionNode) nodesList.get(i));
                    ((PositionNode) (nodesList.get(i))).setPrevNode((PositionNode) nodesList.get(i - 1));
                }
//				for (int i = 1; i < nodesList.size(); i++) {
                if (converter.desc()) {
                    for (int i = nodesList.size() - 1; i > 0; i--) {
                        converter.process((PositionNode) nodesList.get(i - 1), (PositionNode) nodesList.get(i));
                    }
                } else {
                    for (int i = 1; i < nodesList.size(); i++) {
                        converter.process((PositionNode) nodesList.get(i - 1), (PositionNode) nodesList.get(i));
                    }
                }
            }
        }
        NodeMap map = new NodeMap();
        for (Chunk ann : annsSet) {
            map.putChunk(ann);
        }
        map.process();
        return map.getList();
    }


    public static String toString(List<Node> node) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, nodeSize = node.size(); i < nodeSize; i++) {
            Node n = node.get(i);
            sb.append("n" + i).append(" ");
            for (Edge e : CONTEXT.getEdges(n)) {
                ChunkEdge ee = (ChunkEdge) e;
                int ind = node.indexOf(CONTEXT.getEndNode(ee));
                sb.append(ee.getAnn().type + "[").append(StringUtils.substring(ee.getAnn().getContent(), 0,
                        100)).append("] n").append(ind).append(", ");
            }
            sb.setLength(sb.length() - 1);
            sb.append("\n");
        }
        return sb.toString();
    }


    public Collection<Chunk> chunk(TextWithChunks chunkText) {
        Collection<Chunk> result = Chunkers.newSet();
        Collection<Chunk> chunks = Chunkers.newSet();
        if (workOnCategories.isEmpty()) {
            chunks.addAll(chunkText);
        } else {
            chunks.addAll(chunkText.retrieve(workOnCategories));
        }
        //add start and end
        chunks.add(new Chunk(chunkText, START, 0, 0));
        chunks.add(new Chunk(chunkText, END, chunkText.getContent().length(), chunkText.getContent().length()));
        //add chunk from preprocessors
        for (Chunker ch : preprocessors) chunks.addAll(ch.chunk(chunkText));

        List<Node> node = toNode(chunks);
        if (node == null || node.isEmpty()) return result;
        if (debugString) System.out.println("DEBUG STRING:" + toString(node));
        List<Match> results = (matchFromAllPosition || matchAll) ? findFromAllPosition(matcher,
                node) : findAllLongest(matcher, node);
        for (Match m : results) {
            buildChunks(m, result, chunkText);
        }
        return result;
    }

    private void buildChunks(Match m, Collection<Chunk> result, TextWithChunks text) {
        if (rootCategory != null) {
            result.add(build(m.getList(), rootCategory, text));
        }
        for (MarkedMatch mm : getAllMarked(m)) {
            if (mm.markMatcher.action != null) {
                Chunk chunk = mm.markMatcher.action.
                        doAction(new GraphMatchWrapper(mm), build(mm.getList(), mm.markMatcher.groupName, text));
                if (chunk != null) result.add(chunk);
            } else if (mm.markMatcher.generateAnnot) {
                result.add(build(mm.getList(), mm.markMatcher.groupName, text));
            }
        }
    }

    static List<MarkedMatch> getAllMarked(Match m) {
        List<MarkedMatch> result = Lists.newArrayList();
        LinkedList<Match> matches = new LinkedList<Match>();
        matches.addLast(m);
        while (!matches.isEmpty()) {
            Match match = matches.removeFirst();
            if (match instanceof MarkedMatch) {
                result.add((MarkedMatch) match);
            }
            for (Match mm : match.getSubMatches()) matches.add(mm);
        }
        return result;
    }

    private Chunk build(List<Edge> edge, String cat, TextWithChunks text) {
        List<ChunkEdge> chunks = (List) edge;
        Chunk an = new Chunk(text, cat,
                Iterables.get(chunks, 0).getAnn().start,
                Iterables.getLast(chunks).getAnn().end
        );
        return an;
    }

    private List<Match> findFromAllPosition(Matcher matcher, List<Node> node) {
        List<Match> result = Lists.newArrayList();
        for (Node n : node) {
            Match m = null;
            MatchResult matchResult = matcher.find(n, CONTEXT);
            if (matchAll) {
                result.addAll(matchResult.getAllMatches());
            } else {
                m = matchResult.next();
                if (m != null) result.add(m);
            }
        }
        return result;
    }

    private List<Match> findAllLongest(Matcher matcher, List<Node> node) {
        List<Match> result = Lists.newArrayList();
        int i = 0;
        while (i < node.size()) {
            Match m = null;
            while (m == null && i < node.size()) {
                Node n = node.get(i++);
                MatchResult matchResult = matcher.find(n, CONTEXT);
                m = matchResult.next();
            }
            if (m != null) {
                List<Edge> edges = m.getList();
                Edge last = Iterables.getLast(edges);
                if (last == null) {
                    break;
                } else {
                    result.add(m);
                    Node endNode = CONTEXT.getEndNode(last);
                    for (int j = i; j < node.size(); ++j) {
                        if (endNode == node.get(j)) {
                            i = j;
                            break;
                        }
                    }
                }

            } else {
                break;
            }
        }
        return result;
    }


    public static class ChunkEdge implements Edge, Comparable<ChunkEdge> {
        private Chunk ann;
        Node endNode;

        public ChunkEdge(Chunk ann, Node endNode) {
            this.endNode = endNode;
            this.ann = ann;
        }

        public Chunk getAnn() {
            return ann;
        }

        @Override
        public String toString() {
            return "ChunkEdge{" + ann + '}';
        }


        public int compareTo(ChunkEdge o) {
            return getAnn().start - o.getAnn().start;
        }
    }

    public static class PositionNode implements Node {
        private int position;
        List<ChunkEdge> edges = Lists.newArrayList();
        private PositionNode prevNode;
        private PositionNode nextNode;

        public PositionNode(int position) {
            this.position = position;
        }

        public int getPosition() {
            return position;
        }


        List<ChunkEdge> getAnnEdges() {
            return edges;
        }

        public String toString() {
            return "PositionNode{" + edges.toString() + "}";
        }

        public PositionNode getNextNode() {
            return nextNode;
        }

        public void setNextNode(PositionNode nextNode) {
            this.nextNode = nextNode;
        }

        public PositionNode getPrevNode() {
            return prevNode;
        }

        public void setPrevNode(PositionNode prevNode) {
            this.prevNode = prevNode;
        }
    }

    public static class MarkedMatch extends Match {
        private Match delegate;
        final MarkMatcher markMatcher;

        public MarkedMatch(Match delegate, MarkMatcher markMatcher) {
            this.delegate = delegate;
            this.markMatcher = markMatcher;
        }

        @Override
        public List<Edge> getList() {
            return delegate.getList();
        }

        @Override
        public List<Match> getSubMatches() {
            return Collections.singletonList(delegate);
        }

        @Override
        public Node getEndNode() {
            return delegate.getEndNode();
        }

        @Override
        public Node getStartNode() {
            return delegate.getStartNode();
        }

        @Override
        public GraphContext getContext() {
            return delegate.getContext();
        }

        @Override
        public String toString() {
            return "M{" + delegate +
                    '}';
        }
    }

    public static class MarkMatcher extends Matcher {
        Matcher matcher;
        String groupName = "";
        String ruleName = "";
        private boolean generateAnnot = true;
        private GraphMatchAction action;

        public MarkMatcher(String groupName, Matcher matcher) {
            this.groupName = groupName;
            this.matcher = matcher;
        }

        public MarkMatcher setGenerateAnnot(boolean generateAnnot) {
            this.generateAnnot = generateAnnot;
            return this;
        }

        public MarkMatcher setAction(GraphMatchAction action) {
            this.action = action;
            return this;
        }

        @Override
        public MatchResult find(final Node node, final GraphContext context) {
            return new MatchResult() {
                MatchResult delegate = matcher.find(node, context);

                @Override
                protected Match nextInner() {
                    Match m = delegate.next();
                    return m == null ? m : new MarkedMatch(m, MarkMatcher.this);
                }
            };
        }

        @Override
        public List<Matcher> getSubMatchers() {
            return Collections.singletonList(matcher);
        }

        public MarkMatcher setRuleName(String ruleName) {
            this.ruleName = ruleName;
            return this;
        }
    }


    //short keys


    /**
     * first we execute matcher and then check result by predicate
     *
     * @param matcher
     * @param annsPred
     * @return
     */
    public static Matcher match(final Matcher matcher, final Predicate<? super GraphMatchWrapper> annsPred) {
        return new GroupPredicateMatcher(new Predicate<Match>() {
            public boolean evaluate(Match match) {
                return annsPred.evaluate(new GraphMatchWrapper(match));
            }
        }, matcher);
    }

    static List<Chunk> toList(List<Edge> edges) {
        List<Chunk> chunks = Lists.newArrayList();
        for (ChunkEdge edge : (List<ChunkEdge>) (List) edges) {
            chunks.add(edge.getAnn());
        }
        return chunks;
    }

    public static Matcher match(String... categories) {
        if (categories.length == 0) return match(PredicateUtils.<Chunk>truePredicate());
        return matchCategories(PredicateUtils.<Chunk>truePredicate(), categories);
    }

    public static Matcher matchRegexp(String... regexps) {
        return matchCategories(PredicateUtils.<Chunk>truePredicate(), regexps).setRegExps(true);
    }

    public static Matcher emptyMatch() {
        return times(match(), 0, 0);
    }

    public static Predicate<Node> toPredicate(final Matcher matcher) {
        return toPredicate(matcher, CONTEXT);
    }

    public static Matcher match(final Predicate<Chunk> pred) {
        return new PredicateMatcher(new Predicate<ChunkEdge>() {
            public boolean evaluate(ChunkEdge edge) {
                return pred.evaluate(edge.getAnn());
            }
        });
    }


//    public static Predicate<GraphMatchWrapper> toPredicateInside(Matcher matcher) {
//        final Matcher any = seq(star(nextNodeMatcher()).relucant(), matcher);
//        return new Predicate<GraphMatchWrapper>() {
//            public boolean evaluate(GraphMatchWrapper match) {
//                Node startNode = match.match.getStartNode();
//                Node endNode = match.match.getEndNode();
//                //case of empty match
//                if (startNode == null || endNode == null) {
//                    return false;
//                }
//                InnerGraphMatcher.InsideContext context = new InnerGraphMatcher.InsideContext(CONTEXT,
// (PositionNode) endNode);
//                return any.find(startNode, context).next() != null;
//            }
//        };
//    }

    private static PredicateMatcherWithCats matchCategories(final Predicate<Chunk> pred, String... categories) {
        final Set<String> cats = Sets.newHashSet(categories);
        return new PredicateMatcherWithCats(new Predicate<ChunkEdge>() {
            public boolean evaluate(ChunkEdge edge) {
                return cats.contains(edge.getAnn().type) && pred.evaluate(edge.getAnn());
            }
        }, cats);
    }


    public static Matcher matchPredicate(final org.apache.commons.collections15.Predicate<? extends Chunk> predicate,
                                         final String... category) {
        return matchCategories(new Predicate<Chunk>() {
            public boolean evaluate(Chunk annotation) {
                return ((Predicate<Chunk>) predicate).evaluate(annotation);
            }
        }, category);
    }

    public static MarkMatcher mark(String category, final Matcher m) {
        return new MarkMatcher(category, m);
    }

    public static MarkMatcher group(String category, final Matcher m) {
        return new MarkMatcher(category, m).setGenerateAnnot(false);
    }

    public static GraphRegExpExt.AStarMatcher selectBest(final Matcher... mm) {
        return new GraphRegExpExt.AStarMatcher(Arrays.asList(mm));
    }

    public static InnerGraphMatcher insideFind(Matcher condition, Matcher matcher) {
        Matcher any = star(nextNodeMatcher()).reluctant();
        return new InnerGraphMatcher(condition, seq(any, matcher));
    }


    /**
     * match innerMatcher in container and continue matchin after container.
     * Note: container will be matched in any case even if there are no innerMatcher
     *
     * @param container
     * @param innerMatcher
     * @return
     */
    public static InnerGraphMatcher insideFindAll(Matcher container, Matcher innerMatcher) {
        Matcher any = star(nextNodeMatcher()).reluctant();
        return new InnerGraphMatcher(container, plus(seq(any, innerMatcher)));
    }

    public static InnerGraphMatcher boundedMatchWithin(GraphRegExp.Matcher containerCategory,
                                                       final GraphRegExp.Matcher innerMatcher) {
        return new InnerGraphMatcher(containerCategory, innerMatcher).setPerformFullMatch();
    }


    public static NextNodeMatcher nextNodeMatcher() {
        return new NextNodeMatcher();
    }

    public static GraphRegExpExt.WeightedMatcher weighted(final Matcher m, final Transformer<GraphMatchWrapper,
            Double> scorer) {
        return new GraphRegExpExt.WeightedMatcher(new Transformer<Match, Double>() {
            public Double transform(Match match) {
                if (scorer == null) return 0d;
                return scorer.transform(new GraphMatchWrapper(match));
            }
        }, m);
    }

    public static Transformer<GraphMatchWrapper, Double> withGroupScorer(final String groupName, final double has,
                                                                         final double hasNot) {
        return new Transformer<GraphMatchWrapper, Double>() {
            public Double transform(GraphMatchWrapper stringListMap) {
                return stringListMap.get(groupName) != null ? has : hasNot;
            }
        };
    }

    private static class PredicateMatcherWithCats extends PredicateMatcher implements CategoriesHolder {
        Set<String> categories;
        private boolean regExps;

        private PredicateMatcherWithCats(Predicate<? extends Edge> matchP, Set<String> categories) {
            super(matchP);
            this.categories = categories;
        }

        @Override
        public Set<String> getCategories() {
            return categories;
        }


        public boolean isRegExps() {
            return regExps;
        }

        public PredicateMatcherWithCats setRegExps(boolean regExps) {
            this.regExps = regExps;
            return this;
        }
    }

    public interface CategoriesHolder {
        Set<String> getCategories();

        boolean isRegExps();
    }

}
