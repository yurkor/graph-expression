package com.myml.gexp.chunker.common.typedef;

import com.google.common.collect.Lists;
import com.myml.gexp.chunker.common.GraphExpChunker;
import com.myml.gexp.chunker.common.GraphMatchWrapper;
import com.myml.gexp.graph.matcher.GraphRegExp;
import com.myml.gexp.graph.matcher.GraphRegExpExt;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.PredicateUtils;
import org.apache.commons.collections15.Transformer;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Author: Yura Korolov
 * Date: 11.02.2011
 * Time: 12:31:04
 */
public class GraphUtils extends GraphExpChunker {
        private GraphUtils(String rootCategory, GraphRegExp.Matcher matcher, String... strs) {
                super(rootCategory, matcher, strs);
        }

        public static GraphRegExp.Matcher regexp(String regexp, GraphRegExp.Matcher matcher) {
                final Pattern pat = Pattern.compile(regexp);
                return match(matcher, new Predicate<GraphMatchWrapper>() {
                        public boolean evaluate(GraphMatchWrapper graphMatchWrapper) {
                                return pat.matcher(graphMatchWrapper.getText()).find();
                        }
                });
        }

        public static GraphRegExp.Matcher regexpNot(String regexp, GraphRegExp.Matcher matcher) {
                final Pattern pat = Pattern.compile(regexp);
                return match(matcher, new Predicate<GraphMatchWrapper>() {
                        public boolean evaluate(GraphMatchWrapper graphMatchWrapper) {
                                return !pat.matcher(graphMatchWrapper.getText()).find();
                        }
                });
        }

        public static GraphRegExp.Matcher selectBest(GraphRegExp.Matcher matcher, int min, int max) {
                List<GraphRegExp.Matcher> matchers = Lists.newArrayList();
                for (int i = 0; i < min; ++i) {
                        matchers.add(matcher);
                }
                for (int i = min; i < max; ++i) {
                        matchers.add(weighted(opt(matcher).reluctant(), SUM_INNER_MATCHES));
                }
                return selectBest(matchers.toArray(new GraphRegExp.Matcher[0]));
        }

        public static GraphRegExpExt.WeightedMatcher weightedBySum(GraphRegExp.Matcher matcher) {
                return weighted(matcher, SUM_INNER_MATCHES);
        }

        public static final Transformer<GraphMatchWrapper, Double> SUM_INNER_MATCHES = new Transformer<GraphMatchWrapper, Double>() {
                public Double transform(GraphMatchWrapper graphMatchWrapper) {
                        List<GraphRegExp.Match> matchList = extractTopLevel(graphMatchWrapper.match, new Predicate<GraphRegExp.Match>() {
                                public boolean evaluate(GraphRegExp.Match match) {
                                        return match instanceof GraphRegExpExt.WeightedMatch;
                                }
                        });
                        double sum = 0;
                        for (GraphRegExp.Match m : matchList) {
                                sum += ((GraphRegExpExt.WeightedMatch) m).getScore();
                        }
                        return sum;
                }
        };

        private static List<GraphRegExp.Match> extractTopLevel(GraphRegExp.Match m, Predicate<GraphRegExp.Match> accept) {
                if (accept.evaluate(m)) {
                        return Collections.singletonList(m);
                } else {
                        List<GraphRegExp.Match> matches = Lists.newArrayList();
                        for (GraphRegExp.Match mm : m.getSubMatches()) {
                                matches.addAll(extractTopLevel(mm, accept));
                        }
                        return matches;
                }
        }

        public static GraphRegExp.Matcher logger(final String name, GraphRegExp.Matcher matcher) {
                return match(matcher, new Predicate<GraphMatchWrapper>() {
                        @Override
                        public boolean evaluate(GraphMatchWrapper graphMatchWrapper) {
                                System.out.printf("%s=%s\n", name, graphMatchWrapper.getText());
                                return true;
                        }
                });
        }

        public static GraphRegExp.Matcher lookahead(boolean positive, GraphRegExp.Matcher matcher, GraphRegExp.Matcher lookahead) {
                Predicate<GraphRegExp.Node> node = toPredicate(lookahead);
                if (!positive) node = PredicateUtils.notPredicate(node);
                return lookahead(matcher, node);
        }
}
