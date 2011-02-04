package com.myml.gexp.graph.matcher;

import com.google.common.collect.Lists;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.PredicateUtils;
import org.apache.commons.collections15.Transformer;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static com.myml.gexp.graph.matcher.GraphRegExp.*;

public class GraphRegExpTest extends Assert {
    static GraphContext context = new GraphContext() {
        public Iterable<Edge> getEdges(GraphRegExp.Node node) {
            return (List)((NodeStr)node).edges;
        }

        public GraphRegExp.Node getEndNode(Edge edge) {
            return ((EdgeStr)edge).end;
        }

        public <T> T put(String key, T value) {
            return null;
        }

        public <T> T get(String key) {
            return null;
        }
    };
	@Test
	public void test() {			//aaaaaccccccccccccccbbbb (c,a)
//        (a,c,c),(a,c,),(a,a,c),(a,a),(a)
//        (a,c,c)(a,c)

		//sequence define
		//a[i1](abc)[i2](cde)[i3]b
		Node i0 = init();
		Matcher m;
		List<List<Edge>> listList;
		//matcher a,c,c
		m = new Seq(Lists.<Matcher>newArrayList(
			new PredicateMatcher(new PredicteStr("a"))
			, new PredicateMatcher(new PredicteStr("c"))
			, new PredicateMatcher(new PredicteStr("c"))
		));

		listList = m.find(i0, context).getAll();
		assertEquals("[[a, c, c]]", listList.toString());
//

//

		m = new Seq(Lists.<Matcher>newArrayList(
			new PredicateMatcher(new PredicteStr("a"))
			, new Star(new Or(Lists.<Matcher>newArrayList(new PredicateMatcher(new PredicteStr("c")), new PredicateMatcher(new PredicteStr("b")))), 1, 100)
			, new PredicateMatcher(new PredicteStr("b"))

		));
		listList = m.find(i0, context).getAll();
		assertEquals("[[a, c, c, b], [a, b, c, b]]", listList.toString());
//
//        //////////////
//
		m = new Seq(Lists.<Matcher>newArrayList(
			new PredicateMatcher(new PredicteStr("a"))
			, new Or(Lists.<Matcher>newArrayList(new PredicateMatcher(new PredicteStr("c")), new PredicateMatcher(new PredicteStr("a"))))
			, new Or(Lists.<Matcher>newArrayList(new PredicateMatcher(new PredicteStr("c")), new PredicateMatcher(new PredicteStr("e"))))
			, new PredicateMatcher(new PredicteStr("b"))
		));
		listList = m.find(i0, context).getAll();
		assertEquals("[[a, c, c, b], [a, c, e, b], [a, a, c, b], [a, a, e, b]]", listList.toString());

		m = new Star(        //(c|a)
			new Or(Lists.<Matcher>newArrayList(new PredicateMatcher(new PredicteStr("c")), new PredicateMatcher(new PredicteStr("a"))))
			, 0, 10
		);
		listList = m.find(i0, context).getAll();
//        assertEquals("[[a, c, c], [a, a, c]]", listList.toString());
        //[[a, c, c], [a, a, c], [a, c], [a, a], [a], []]
		assertEquals("[[a, c, c], [a, c], [a, a, c], [a, a], [a], []]", listList.toString());

		m = new Star(
			new Or(Lists.<Matcher>newArrayList(new PredicateMatcher(new PredicteStr("c")), new PredicateMatcher(new PredicteStr("a"))))
			, 0, 10
		).setGreedy(false);
		listList = m.find(i0, context).getAll();
        //
        //[[], [a], [a, c], [a, a], [a, c, c], [a, a, c]]
		assertEquals("[[], [a], [a, c], [a, c, c], [a, a], [a, a, c]]", listList.toString());
//

		m = new Star(
			new Or(Lists.<Matcher>newArrayList(new PredicateMatcher(new PredicteStr("c")), new PredicateMatcher(new PredicteStr("a"))))
			, 1, 2
		);
		listList = m.find(i0, context).getAll();
		assertEquals("[[a, c], [a, a], [a]]", listList.toString());
//
//        //matcher a,c,c
		m = new Seq(Lists.<Matcher>newArrayList(
			new PredicateMatcher(new PredicteStr("c"))
			, new Or(Lists.<Matcher>newArrayList(new PredicateMatcher(new PredicteStr("c")), new PredicateMatcher(new PredicteStr("b"))))
		));
		listList = m.find(i0, context).getAll();
		assertEquals("[]", listList.toString());

		m = new Star(
			new PredicateMatcher(PredicateUtils.<Edge>truePredicate()), 0, 0
		);
		listList = m.find(i0, context).getAll();
		assertEquals("[[]]", listList.toString());
//        listList = m.findAll(i0);
//        assertEquals("[[c, c], [c, b]]", listList.toString());
	}

	private Node init() {
		NodeStr i0 = new NodeStr("i0");
		NodeStr i1 = new NodeStr("i1");
		NodeStr i2 = new NodeStr("i2");
		NodeStr i3 = new NodeStr("i3");
		NodeStr i4 = new NodeStr("i4");

		EdgeStr a1 = new EdgeStr("a", i1);
		i0.add(a1);

		EdgeStr a2 = new EdgeStr("a", i2);
		EdgeStr b1 = new EdgeStr("b", i2);
		EdgeStr c1 = new EdgeStr("c", i2);
		i1.add(a2);
		i1.add(b1);
		i1.add(c1);

		EdgeStr c2 = new EdgeStr("c", i3);
		EdgeStr d1 = new EdgeStr("d", i3);
		EdgeStr e1 = new EdgeStr("e", i3);
		i2.add(c2);
		i2.add(d1);
		i2.add(e1);

		EdgeStr b2 = new EdgeStr("b", i4);
		i3.add(b2);
		return i0;
	}

	@Test
	public void testAStar() {
		//a[(abc)(cde)b
		Node i0 = init();
		Matcher m;
		List<List<Edge>> listList;
		Matcher any = new Star(new PredicateMatcher(PredicateUtils.<Edge>truePredicate()), 1, 2);
		class LengthWeight implements Transformer<Match, Double> {
			int dist = 0;

			LengthWeight(int dist) {
				this.dist = dist;
			}

			public Double transform(Match match) {
				return (double) Math.abs(match.getList().size() - dist);
			}
		}
		m = new GraphRegExpExt.AStarMatcher(Lists.<GraphRegExpExt.Matcher>newArrayList(
			new GraphRegExpExt.WeightedMatcher(new LengthWeight(2), any),
			new GraphRegExpExt.WeightedMatcher(new LengthWeight(2), any)
		));
		listList = m.find(i0, context).getAll();
		System.out.println(listList.size());
		assertEquals(30, listList.size());
		assertEquals("[[a, a, c, b], [a, a, d, b], [a, a, e, b], [a, b, c, b], [a, b, d, b], [a, b, e, b], [a, c, c, b], [a, c, d, b], [a, c, e, b], [a, a, c], [a, a, d], [a, a, e], [a, b, c], [a, b, d], [a, b, e], [a, c, c], [a, c, d], [a, c, e], [a, a, c], [a, a, d], [a, a, e], [a, b, c], [a, b, d], [a, b, e], [a, c, c], [a, c, d], [a, c, e], [a, a], [a, b], [a, c]]", listList.toString());
		m = new GraphRegExpExt.AStarMatcher(Lists.<GraphRegExpExt.Matcher>newArrayList(
			new GraphRegExpExt.WeightedMatcher(new LengthWeight(2), any).setBestFirst(true),
			new GraphRegExpExt.WeightedMatcher(new LengthWeight(2), any).setBestFirst(true)
		));
		listList = m.find(i0, context).getAll();
		System.out.println(listList.size());
		assertEquals(30, listList.size());
	}

	public class PredicteStr implements Predicate<EdgeStr> {
		String str;

		PredicteStr(String str) {
			this.str = str;
		}

		public boolean evaluate(EdgeStr edgeStr) {
			return edgeStr.tag.matches(str);
		}
	}

	public static class NodeStr implements Node {
		String tag;
        List<EdgeStr>  edges = Lists.newArrayList();

		public NodeStr(String tag) {
			this.tag = tag;
		}

		@Override
		public String toString() {
			return tag;
		}

        public void add(EdgeStr str) {
               edges.add(str);
        }
	}

	public static class EdgeStr implements Edge {
		String tag;
        Node end;

		public EdgeStr(String tag, Node end) {
			this.tag = tag;
            this.end = end;
		}

		@Override
		public String toString() {
			return tag;
		}
	}


}
