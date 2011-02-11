package com.myml.gexp.graph.matcher;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.collections15.Transformer;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

/**
 * Author: Yura Korolov
 * Date: 13.10.2010
 * Time: 15:38:48
 */
public class GraphRegExpExt extends GraphRegExp {
	public static class WeightedMatch extends Match implements Comparable<WeightedMatch> {
		Match match;
		double score;

		public WeightedMatch(Match match, double score) {
			this.match = match;
			this.score = score;
		}

		@Override
		public List<Edge> getList() {
			return match.getList();
		}

		@Override
		public List<Match> getSubMatches() {
			return Collections.singletonList(match);
		}

		@Override
		public Node getEndNode() {
			return match.getEndNode();
		}

        @Override
        public Node getStartNode() {
            return match.getStartNode(); 
        }

        @Override
        public GraphContext getContext() {
            return match.getContext();
        }

        public double getScore() {
			return score;
		}

		public int compareTo(WeightedMatch o) {
			return Double.compare(this.score, o.score);
		}
	}

	public static class WeightedMatcher extends Matcher {
		private Transformer<Match, Double> scorer;
		private Matcher innedMatcher;
		private boolean bestFirst = false;
                private boolean showBest = false;

		public WeightedMatcher(Transformer<Match, Double> scorer, Matcher innedMatcher) {
			this.scorer = scorer;
			this.innedMatcher = innedMatcher;
		}

		public WeightedMatcher setBestFirst(boolean bestFirst) {
			this.bestFirst = bestFirst;
			return this;
		}

		public MatchResult find(final Node node,final GraphContext context) {
			if (!bestFirst) {
				return new MatchResult() {
					MatchResult result = innedMatcher.find(node, context);

					protected Match nextInner() {
						if (result.next() != null)
							return new WeightedMatch(result.current(), scorer.transform(result.current()));
						return null;
					}

                                        public boolean isBestFirst() {
                                                return showBest;
                                        }
                                };

			} else {
				return new MatchResult() {
					Iterator<WeightedMatch> matchIter;
					MatchResult inner;

					protected Match nextInner() {

						init();
						if (matchIter.hasNext()) {
							return matchIter.next();
						}
						return null;

					}

					private void init() {
						if (matchIter != null) return;
						List<WeightedMatch> matches = Lists.newArrayList();
						MatchResult result = innedMatcher.find(node, context);
						Match m;
						while ((m = result.next()) != null) {
							matches.add(new WeightedMatch(m, scorer.transform(m)));
						}
						Collections.sort(matches);
						matchIter = matches.iterator();
					}

					public boolean isBestFirst() {
						return true;
					}
				};
			}

		}

		public List<Matcher> getSubMatchers() {
			return Collections.singletonList(innedMatcher);
		}

                public WeightedMatcher setShowBestFist(boolean b) {
                        showBest = b;
                        return this;
                }
        }

	/**
	 * minimize weights
	 */
	public static class AStarMatcher extends Matcher {
		List<Matcher> matchers;
		double maxValue = Double.POSITIVE_INFINITY;

		public AStarMatcher setMaxValue(double maxValue) {
			this.maxValue = maxValue;
			return this;
		}

		public AStarMatcher(List<Matcher> matchers) {
			this.matchers = matchers;
		}

		@Override
		public MatchResult find(final Node node,final  GraphContext context) {
			return new MatchResult() {

				@Override
				public boolean isBestFirst() {
					return true;
				}

				List<Match> matcher;
				int idState = 0;

				class Path extends CompositeMatch implements Comparable<Path> {
					double score;
					int position;
					int id = idState++;
					MatchResult res;

					private Path(double score, int position, Match... match) {
						super(context, match);
						this.score = score;
						this.position = position;
//						System.out.println(id);
					}

					public int compareTo(Path o) {
						int res = Double.compare(score, o.score);
						if (res == 0) return id - o.id;
						return res;
					}

					boolean isFinal() {
						return position == matchers.size();
					}

					     public Path addMatchToNew(Match m) {
                                                List<Match> matches = Lists.newArrayList(getSubMatches());
                                                matches.add(m);
                                                return new Path(score + eval(m), position + 1, matches.toArray(new Match[matches.size()]));
                                        }

                                        public Path changeLastToNew(Match m) {
                                                List<Match> matches = Lists.newArrayList(getSubMatches());
                                                Match prev = Iterables.getLast(matches);
                                                matches.set(matches.size() - 1, m);
                                                Path newPath = new Path(score + eval(m) - eval(prev), position, matches.toArray(new Match[matches.size()]));
                                                newPath.res = this.res;
                                                return newPath;
                                        }

                                        private double eval(Match m) {
                                                if (m instanceof WeightedMatch) {
                                                        return ((WeightedMatch) m).score;
                                                }
                                                return 0;
                                        }
				}

				SortedSet<Path> set;

				{
					set = Sets.newTreeSet();
					set.add(new Path(0, 0, new CompositeMatch(context)));
				}

				double prevBest = Double.POSITIVE_INFINITY;

				@Override
				protected Match nextInner() {
					        Path currentMatch = null;
                                        while (!set.isEmpty()) {
                                                Iterator<Path> pathQueueIterator = set.iterator();
                                                currentMatch = pathQueueIterator.next();
                                                pathQueueIterator.remove();
                                                //means result match is best first
                                                if (currentMatch.res != null && currentMatch.res.isBestFirst() && currentMatch.res.next() != null) {
                                                        set.add(currentMatch.changeLastToNew(currentMatch.res.current()));
                                                }

                                                if (currentMatch.isFinal()) break;
                                                Node n = currentMatch.getEndNode();
                                                if (n == null) n = node;
                                                MatchResult nextMatchResult = matchers.get(currentMatch.position).find(n, context);
                                                if (nextMatchResult.isBestFirst()) {
                                                        //open only first
                                                        if (nextMatchResult.next() != null) {
                                                                Path newPath = currentMatch.addMatchToNew(nextMatchResult.current());
                                                                newPath.res = nextMatchResult;
                                                                set.add(newPath);
                                                        }


                                                } else {
                                                        //open all nodes
                                                        for (Match m : nextMatchResult.getAllMatches()) {
                                                                Path newPath = currentMatch.addMatchToNew(m);
                                                                set.add(newPath);
                                                        }
                                                }
                                        }
                                        if (currentMatch == null || !currentMatch.isFinal() || currentMatch.score >= maxValue) {
                                                set.clear();
                                                return null;
                                        } else return new WeightedMatch(currentMatch, currentMatch.score);
				}
			};
		}

		@Override
		public List<Matcher> getSubMatchers() {
			return (List) matchers;
		}
	}
}
