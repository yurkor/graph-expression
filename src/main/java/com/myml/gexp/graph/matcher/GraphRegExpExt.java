package com.myml.gexp.graph.matcher;

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
						double newScore = 0;
						if(m instanceof WeightedMatch) {
							newScore = ((WeightedMatch)m).score;
						}
						return new Path(score + newScore, position + 1, matches.toArray(new Match[matches.size()]));
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
					Path p = null;
					while (!set.isEmpty()) {
						Iterator<Path> pi = set.iterator();
						p = pi.next();
						pi.remove();
						//means result match is best first
						if (p.res != null && p.res.next() != null) {
							Path newPath = p.addMatchToNew(p.res.current());
							newPath.res = p.res;
							newPath.position--; //we on same position as before
							set.add(newPath);
						}

						if (p.isFinal()) break;
						Node n = p.getEndNode();
						if (n == null) n = node;
						MatchResult res = matchers.get(p.position).find(n, context);
						if (res.isBestFirst()) {
							//open only first
							if (res.next() != null) {
								Path newPath = p.addMatchToNew(res.current());
								newPath.res = res;
								set.add(newPath);
							}


						} else {
							//open all nodes
							for (Match m : res.getAllMatches()) {
								Path newPath = p.addMatchToNew(m);
								set.add(newPath);
							}
						}
					}
					if (p == null || !p.isFinal() || p.score >= maxValue) {
						set.clear();
						return null;
					} else return p;
				}
			};
		}

		@Override
		public List<Matcher> getSubMatchers() {
			return (List) matchers;
		}
	}
}
