package com.myml.gexp.graph.matcher;

import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.PredicateUtils;

import java.util.Arrays;

import static com.myml.gexp.graph.matcher.GraphRegExp.*;

/**
 * Author: java developer 1
 * Date: 04.02.2011
 * Time: 15:00:48
 */
public class GraphRegExpMatchers {

    public static GraphRegExp.Matcher seq(GraphRegExp.Matcher... ms) {
        return new Seq(Arrays.asList(ms));
    }

    public static Star times(Matcher ms, int min, int max) {
        return new Star(ms, min, max);
    }

    public static Star plus(Matcher ms) {
        return new Star(ms, 1, Integer.MAX_VALUE);
    }

    public static Star star(Matcher ms) {
        return new Star(ms, 0, Integer.MAX_VALUE);
    }

    public static Star opt(Matcher ms) {
        return new Star(ms, 0, 1);
    }

    public static Matcher or(Matcher... ms) {
        return new Or(Arrays.asList(ms));
    }

    public static Matcher cut(Matcher matcher) {
        return new CutMatcher(matcher);
    }

    /**
     * fist we do lookahed then execute predicate
     *
     * @param matcher
     * @param p
     * @return
     */
    public static LookaheadMatcher lookahead(Matcher matcher, Predicate<? super Node> p) {
        return new LookaheadMatcher(matcher, (Predicate<Node>) p);
    }

    public static GraphRegExpExt.AStarMatcher selectBest(final Matcher... mm) {
        return new GraphRegExpExt.AStarMatcher(Arrays.asList(mm));
    }

    public static Matcher match(final Matcher matcher, final Predicate<Match> annsPred) {
        return new GroupPredicateMatcher(annsPred, matcher);
    }

    public static Predicate<Node> toPredicate(final Matcher matcher, final GraphContext context) {
        return new Predicate<Node>() {
            public boolean evaluate(Node node) {
                return matcher.find(node, context).next() != null;
            }
        };
    }
}
