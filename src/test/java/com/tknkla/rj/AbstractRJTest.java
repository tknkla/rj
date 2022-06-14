/* MIT License
 *
 * Copyright (c) 2022 TKNKLA
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.tknkla.rj;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.function.IntBinaryOperator;

import org.junit.Test;

import com.tknkla.rj.functions.IntBinaryPredicate;
import com.tknkla.rj.groups.GroupOperator;
import com.tknkla.rj.groups.IntGroupOperator;
import com.tknkla.rj.groups.LongGroupOperator;

public abstract class AbstractRJTest extends TestSupport {

	public AbstractRJTest(ExecutionStrategy xs) {
		super();
		RJ.setExecutor(xs);
	}
	
	@Test
	public void testPopulate() {
		int[] vs = new int[] {0,1,3,5,7};
		assertEquals(vs,
				RJ.populate(vs.length, (int p) -> vs[p]),
				RJ.populate(vs.length, (int p) -> (long)vs[p]),
				RJ.populate(BigInteger.class, vs.length, (int p) -> BigInteger.valueOf(vs[p])));
	}

	public void testJoin(int[] expected, int[]... as) {
		assertEquals(expected,
				RJ.joinAsInt(as.length, (int p) -> as[p]),
				RJ.joinAsLong(as.length, (int p) -> toLong(as[p])),
				RJ.join(BigInteger.class, as.length, (int p) -> toBigInteger(as[p])));
	}
	
	@Test
	public void testJoin() {
		testJoin(new int[0], new int[0]);
		testJoin(new int[] { 0,1,0,2,3,5,5,5 }, new int[] { 0,1 }, new int[] { 0,2,3 }, new int[0], new int[] {5,5,5});
	}

	
	public void testOrder1(int[] expected, int[] src) {
		assertEquals(expected,
				RJ.order(src.length, (int p) -> src[p], Integer::compare, (int a, int b) -> a),
				RJ.order(src.length, (int p) -> (long)src[p], Long::compare, (long a, long b) -> a),
				RJ.order(BigInteger.class, src.length, (int p) -> BigInteger.valueOf(src[p]), Comparator.naturalOrder(), (BigInteger a, BigInteger b) -> a));
	}
	
	@Test
	public void testOrder1() {
		testOrder1(new int[0], new int[0]);
		testOrder1(new int[] {0,1,2,3}, new int[] {3,2,3,0,1,1});
	}

	public void testGroups1(int[][] expected, int[] src, int m) {
		IntBinaryOperator cmp = (int a, int b) -> Integer.compare(a%m, b%m);
		assertEquals(expected,
				RJ.groups(src.length, (int p) -> src[p], cmp, Integer::compare),
				RJ.groups(src.length, (int p) -> (long)src[p],
						(long a, long b) -> cmp.applyAsInt((int)a, (int)b),
						Long::compare),
				RJ.groups(BigInteger.class, src.length, (int p) -> BigInteger.valueOf(src[p]),
						(BigInteger a, BigInteger b) -> cmp.applyAsInt(a.intValueExact(), b.intValueExact()),
						Comparator.naturalOrder()));
	}
	
	@Test
	public void testGroups1() {
		testGroups1(new int[0][], new int[0], 2);
		testGroups1(new int[][] {{0,2,4,6},{1,3,5,7}}, new int[] {0,1,2,3,3,4,4,5,6,7}, 2);
		testGroups1(new int[][] {{0,3,6},{1,4,7},{2,5}}, new int[] {0,1,2,3,3,4,4,5,6,7}, 3);
	}

	public void testGroups2(int[][] expected, int[][] src, int m) {
		IntBinaryOperator cmp = (int a, int b) -> Integer.compare(a%m, b%m);
		assertEquals(expected,
				RJ.groups(src, cmp, Integer::compare),
				RJ.groups(toLong(src),
						(long a, long b) -> cmp.applyAsInt((int)a, (int)b),
						Long::compare),
				RJ.groups(BigInteger.class, toBigInteger(src),
						(BigInteger a, BigInteger b) -> cmp.applyAsInt(a.intValueExact(), b.intValueExact()),
						Comparator.naturalOrder()));
	}
	
	@Test
	public void testGroups2() {
		testGroups2(new int[0][], new int[0][], 2);
		testGroups2(new int[][] {{0,6},{4},{2},{3},{1,7},{5},{8}}, new int[][] {{0,2,4,6},{1,3,5,7},{8}}, 3);
		testGroups2(new int[][] {{0,6},{3},{4},{1,7},{2},{5},{8}}, new int[][] {{0,3,6},{1,4,7},{2,5},{8}}, 2);
	}

	/* TODO testitapaukset?
	public void testOrder(int[] expected, IntBinaryOperator fg) {
		int[][] src = new int[][] { expected.clone() };
		Arrays.sort(src[0]);
		int[] rt = order(src, fg); 
		assertArrayEquals(expected, rt);
	}*/
	
	private int[] order(int[][] src, IntBinaryOperator fg) {
		long t0 = System.nanoTime();
		int[] rti = RJ.order(src, fg, IntGroupOperator.ADDITIVE, Integer::compare);
		t0 = System.nanoTime()-t0;
		
		long t1 = System.nanoTime();
		long[] rtl = RJ.order(toLong(src), (long a, long b) -> fg.applyAsInt((int)a, (int)b), LongGroupOperator.ADDITIVE, Long::compare);
		t1 = System.nanoTime()-t1;

		long t2 = System.nanoTime();
		BigInteger[] rto = RJ.order(BigInteger.class, toBigInteger(src),
				(BigInteger a, BigInteger b) -> BigInteger.valueOf(fg.applyAsInt(a.intValueExact(), b.intValueExact())),
				GroupOperator.BIGINTEGER_ADDITIVE,
				Comparator.naturalOrder());
		t2 = System.nanoTime()-t2;

		System.out.println(Arrays.deepToString(src)+" ?=\n\t"+
				"("+t0+") "+Arrays.toString(rti)+"\n\t"+
				"("+t1+") "+Arrays.toString(rtl)+"\n\t"+
				"("+t2+") "+Arrays.toString(rto)+"\n\t");

		assertArrayEquals(rti, toInt(rtl));
		assertArrayEquals(rti, toInt(rto));
		return rti;
	}	

	public void testOrder(int ln, IntBinaryOperator fa, IntBinaryOperator fb, Random rnd) {
		int[][] src = new int[][] { RJ.populate(ln, (int p) -> p) };
		RJ.shuffle(src[0], rnd);
		int[] ra = order(src, fa); 
		int[] rb = order(src, fb);
		int fails=0;
		for (int i=0; i<ln; i++) {
			for (int j=0; j<ln; j++) {
				int va = fa.applyAsInt(ra[i], ra[j]), vb = fb.applyAsInt(rb[i], rb[j]);
				System.out.print((va>0?"<":".")+(vb>0?">":".")+" ");
				if (va!=vb) {
					fails++;
				}
			}
			System.out.println();
		}
		System.out.println(fails);
		assertEquals(0, fails);
	}
	
	public void testOrder2(int ln, long seed) {
		Random rnd = new Random(seed);
		int[] oa = RJ.populate(ln, (int p) -> p);
		int[] ob = RJ.populate(ln, (int p) -> p);
		RJ.shuffle(oa, rnd);
		RJ.shuffle(ob, rnd);
		IntBinaryPredicate fg = RJ.randomUndirected(ln, Double.NaN, Double.NaN, rnd);
		int[][] g = RJ.populate(ln, ln, RJ.asOperator(fg));
		testOrder(ln,
				(int a, int b) -> g[oa[a]][oa[b]],
				(int a, int b) -> g[ob[a]][ob[b]],
				rnd);
	}
	
	public void testOrder2(BigInteger n, Random rnd) {
		int ln = RJ.verticesOfUndirected(n);
		IntBinaryPredicate fg = RJ.decodeUndirected(n);
		int[] oa = RJ.populate(ln, (int p) -> p);
		int[] ob = RJ.populate(ln, (int p) -> p);
		RJ.shuffle(oa, rnd);
		RJ.shuffle(ob, rnd);
		int[][] g = RJ.populate(ln, ln, RJ.asOperator(fg));
		testOrder(ln,
				(int a, int b) -> g[oa[a]][oa[b]],
				(int a, int b) -> g[ob[a]][ob[b]],
				rnd);
	}

	@Test
	public void testOrder2() {
		Random rnd = new Random();
		testOrder2(new BigInteger("31426386039970470300"), rnd);
		testOrder2(new BigInteger("27860309397548567985"), rnd);
		testOrder2(new BigInteger("12886582265524335"), rnd);
		testOrder2(new BigInteger("19328502063479731"), rnd);
		testOrder2(new BigInteger("176172819"), rnd);
		testOrder2(new BigInteger("109412721"), rnd);
		// TODO ref-testit rassista
	}

	@Test
	public void testOrder2Randoms() {
		Random rnd = new Random(0);
		for (int i=5; i<16; i++) {
			testOrder2(i, rnd.nextLong());
		}
	}

	@Test
	public void testWrap() {
		assertArrayEquals(new String[] { "x" }, RJ.wrap("x"));
	}

	@Test
	public void testWrapType() {
		assertEquals(int[][].class, RJ.wrap(int[].class));
	}

	@Test
	public void testEmpty() {
		assertArrayEquals(new String[0], RJ.empty(String.class));
	}
	
	public static void testJoin2(int[] expected, int[] a, int[] b) {
		assertEquals(expected,
				RJ.join(a,b),
				RJ.join(toLong(a),toLong(b)),
				RJ.join(toBigInteger(a),toBigInteger(b)));
	}
	
	@Test
	public void testJoin2() {
		testJoin2(new int[0], new int[0], new int[0]);
		testJoin2(new int[] { 0,1,0,2,3 }, new int[] { 0,1 }, new int[] { 0,2,3 });
	}

	public static void testMerge(SetOperator op, int[] expected, int[] a, int[] b) {
		assertEquals(expected,
				RJ.merge(a,b, Integer::compare,
						(int u, int v) -> u, op),
				RJ.merge(toLong(a),toLong(b), Long::compare,
						(long u, long v) -> u, op),
				RJ.merge(BigInteger.class, toBigInteger(a),toBigInteger(b), Comparator.naturalOrder(), 
						(BigInteger u, BigInteger v) -> u, op));
		
		if (op.left == op.right) {
			assertEquals(expected,
					RJ.merge(2, (int p) -> p==0 ? a : b, Integer::compare,
							(int u, int v) -> u, op),
					RJ.merge(2, (int p) -> toLong(p==0 ? a : b), Long::compare,
							(long u, long v) -> u, op),
					RJ.merge(BigInteger.class, 2, (int p) -> toBigInteger(p==0 ? a : b), Comparator.naturalOrder(), 
							(BigInteger u, BigInteger v) -> u, op));
		}
	}
	
	@Test
	public void testUnion() {
		testMerge(SetOperator.UNION, new int[0], new int[0], new int[0]);
		testMerge(SetOperator.UNION, new int[] { 0,1,2,3 }, new int[] { 0,1 }, new int[] { 0,2,3 });
		testMerge(SetOperator.UNION, new int[] { 0,1,2,3 }, new int[] { 2,3 }, new int[] { 0,1,3 });
		testMerge(SetOperator.UNION, new int[] { 0,1,2,3 }, new int[] { 0,3 }, new int[] { 1,2 });
	}

	@Test
	public void testIntersection() {
		testMerge(SetOperator.ISECT, new int[0], new int[0], new int[0]);
		testMerge(SetOperator.ISECT, new int[] { 0 }, new int[] { 0,1 }, new int[] { 0,2,3 });
		testMerge(SetOperator.ISECT, new int[] { 3 }, new int[] { 2,3 }, new int[] { 0,1,3 });
		testMerge(SetOperator.ISECT, new int[0], new int[] { 0,3 }, new int[] { 1,2 });
	}

	@Test
	public void testDifference() {
		testMerge(SetOperator.DIFF, new int[0], new int[0], new int[0]);
		testMerge(SetOperator.DIFF, new int[] { 1,2,3 }, new int[] { 0,1 }, new int[] { 0,2,3 });
		testMerge(SetOperator.DIFF, new int[] { 0,1,2 }, new int[] { 2,3 }, new int[] { 0,1,3 });
		testMerge(SetOperator.DIFF, new int[] { 0,1,2,3 }, new int[] { 0,3 }, new int[] { 1,2 });
	}
	
	@Test
	public void testSetOperator() {
		assertEquals(SetOperator.EMPTY, SetOperator.of(false,false,false));
		assertEquals(SetOperator.ISECT, SetOperator.of(false,false,true));
		assertEquals(SetOperator.RDIFF, SetOperator.of(false,true,false));
		assertEquals(SetOperator.RIGHT, SetOperator.of(false,true,true));
		assertEquals(SetOperator.LDIFF, SetOperator.of(true,false,false));
		assertEquals(SetOperator.LEFT, SetOperator.of(true,false,true));
		assertEquals(SetOperator.DIFF, SetOperator.of(true,true,false));
		assertEquals(SetOperator.UNION, SetOperator.of(true,true,true));
	}

	public void testUndirectedCodec(int ln, IntBinaryPredicate fn) {
		BigInteger cv = RJ.encodeUndirected(ln, fn);
		System.out.println("G("+RJ.verticesOfUndirected(cv)+")="+cv.toString(2));
		assertTrue(ln>=RJ.verticesOfUndirected(cv));
		IntBinaryPredicate f2 = RJ.decodeUndirected(cv);
		int fails=0;
		for (int i=0; i<ln; i++) {
			for (int j=0; j<ln; j++) {
				boolean va = fn.test(i, j), vb = f2.test(i, j);
				System.out.print((va?"<":".")+(vb?">":".")+" ");
				if (va!=vb) {
					fails++;
				}
			}
			System.out.println();
		}
		System.out.println(fails);
		assertEquals(0, fails);
	}
	
	public void testUndirectedCodec(int ln, long seed) {
		testUndirectedCodec(ln, RJ.randomUndirected(ln, Double.NaN, Double.NaN, new Random(seed)));
	}
	
	@Test
	public void testUndirectedCodec() {
		Random rnd = new Random(0);
		for (int i=0; i<64; i++) {
			testUndirectedCodec(i, rnd.nextLong());
		}
	}

	public void testExecution(int n) {
		int ev = (n*n-n)/2;
		assertEquals(ev, RJ.execute(0, n, 0, (int p) -> p, (int a, int b) -> a+b));
		assertEquals(ev, RJ.execute(0, n, 0, (int p) -> (long)p, (long a, long b) -> a+b));
		assertEquals(BigInteger.valueOf(ev), RJ.execute(0, n, BigInteger.ZERO, (int p) -> BigInteger.valueOf(p), (BigInteger a, BigInteger b) -> a.add(b)));
	}

	@Test
	public void testExecution() {
		testExecution(1);
		testExecution(10);
		testExecution(100);
		testExecution(1000);
	}

}
