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

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.Predicate;

import com.tknkla.rj.functions.IntBiFunction;
import com.tknkla.rj.functions.IntBinaryConsumer;
import com.tknkla.rj.functions.IntBinaryPredicate;
import com.tknkla.rj.functions.IntToLongBinaryOperator;
import com.tknkla.rj.functions.LongToIntBinaryOperator;
import com.tknkla.rj.groups.GroupOperator;
import com.tknkla.rj.groups.IntGroupOperator;
import com.tknkla.rj.groups.LongGroupOperator;

/**
 * Elementary algorithms and related utilities for relative programming.
 *
 * <p>Majority of the algorithms are parallelized. All such algorithms are marked with (P) in
 * the documentation.</p>
 *
 * @author Timo Santasalo
 * @since 1.0.0
 */
public final class RJ {

	/**
	 * An empty graph; always returns false. 
	 * @since 1.0.0
	 */
	public static final IntBinaryPredicate UNDIRECTED_EMPTY = (int a, int b) -> false;

	/**
	 * A complete graph; returns true for all non-equal pairs of arguments.
	 * @since 1.0.0
	 */
	public static final IntBinaryPredicate UNDIRECTED_COMPLETE = (int a, int b) -> a!=b;

	/**
	 * An empty array of <code>int</code>s.
	 * @since 1.0.0
	 */
	public static final int[] EMPTY_INT = new int[0];
	
	/**
	 * An empty array of <code>long</code>s.
	 * @since 1.0.0
	 */
	public static final long[] EMPTY_LONG = new long[0];
	
	private static ExecutionStrategy xs = ExecutionStrategy.LOCAL;

	private RJ() {}
	
	/* INIT */
	
	/**
	 * Sets the global parallelization strategy.
	 * 
	 * <p>Note that as the parallelization configuration is global, no programming library (using RJ),
	 * but only the final application using RJ either directly or indirectly, should ever configure
	 * the parallelization strategy.</p>
	 * 
	 * @param xs The parallelization strategy.
	 * @see ExecutionStrategy#LOCAL
	 * @see ExecutionStrategy#PARALLEL
	 * @since 1.0.0
	 */
	public static void setExecutor(ExecutionStrategy xs) {
		RJ.xs = xs;
	}

	/**
	 * Returns the global parallelization strategy.
	 * 
	 * @return The parallelization strategy.
	 * @since 1.0.0
	 */
	public static ExecutionStrategy getExecutor() {
		return xs;
	}
	
	/* EXECUTE */
	
	/**
	 * (P) Executes a lambda for all arguments within range (in undefined order).
	 * 
	 * @param from Start of the execution range (inclusive).
	 * @param to End of the execution range (exclusive).
	 * @param fn Lambda to be executed (with all arguments within the execution range).
	 * @since 1.0.0
	 */
	public static void execute(int from, int to, IntConsumer fn) {
		if (xs.fork(to-from)) {
			xs.execute((Runnable r) -> _execute(from, to, fn, r));
		} else {
			_execute(from, to, fn);
		}
	}

	private static void _execute(int from, int to, IntConsumer fn) {
		for (int i=from; i<to; i++) {
			fn.accept(i);
		}
	}
	
	private static void _execute(int from, int to, IntConsumer fn, Runnable fh) {
		int ln = to-from;
		if (ln>1 && xs.fork(ln)) {
			int h = (from+to)>>1;
			xs.queue((Runnable nh) -> _execute(from, h, fn, nh),
					(Runnable nh) -> _execute(h, to, fn, nh),
					fh);
		} else if (ln>0) {
			try {
				_execute(from, to, fn);
			} finally {
				fh.run();
			}
		}
	}
	
	/**
	 * (P) Executes a lambda for all arguments within range (in undefined order)
	 * and combines the result with an associative operator.
	 * 
	 * @param from Start of the execution range (inclusive).
	 * @param to End of the execution range (exclusive).
	 * @param id Identity element (to be returned if <code>from>=to</code>).
	 * @param fv Lambda to executed (with all arguments within the execution range).
	 * @param fm Associative operator (for merging results).
	 * @return Result of the operation.
	 * @since 1.0.0
	 */
	public static int execute(int from, int to, int id, IntUnaryOperator fv, IntBinaryOperator fm) {
		return xs.fork(to-from)
				? xs.executeAsInt((IntConsumer fh) -> _execute(from, to, id, fv, fm, fh))
				: _execute(from, to, id, fv, fm);
	}
	
	private static int _execute(int from, int to, int id, IntUnaryOperator fv, IntBinaryOperator fm) {
		int ret = id;
		for (int i=from; i<to; i++) {
			ret = fm.applyAsInt(ret, fv.applyAsInt(i));
		}
		return ret;
	}

	private static void _execute(int from, int to, int id, IntUnaryOperator fv, IntBinaryOperator fm, IntConsumer fh) {
		if (from>=to) {
			fh.accept(id);
		} else if (from+1==to) {
			fh.accept(fv.applyAsInt(from));
		} else if (xs.fork(to-from)) {
			int h = (from+to)>>1;
			xs.queue((IntConsumer nh) -> _execute(from, h, id, fv, fm, nh),
					(IntConsumer nh) -> _execute(h, to, id, fv, fm, nh),
					fh, fm);
		} else {
			fh.accept(_execute(from, to, id, fv, fm));
		}
	}

	/**
	 * (P) Executes a lambda for all arguments within range (in undefined order)
	 * and combines the result with an associative operator.
	 * 
	 * @param from Start of the execution range (inclusive).
	 * @param to End of the execution range (exclusive).
	 * @param id Identity element (to be returned if <code>from>=to</code>).
	 * @param fv Lambda to executed (with all arguments within the execution range).
	 * @param fm Associative operator (for merging results).
	 * @return Result of the operation.
	 * @since 1.0.0
	 */
	public static long execute(int from, int to, long id, IntToLongFunction fv, LongBinaryOperator fm) {
		return xs.fork(to-from)
				? xs.executeAsLong((LongConsumer fh) -> _execute(from, to, id, fv, fm, fh))
				: _execute(from, to, id, fv, fm);
	}
	
	private static long _execute(int from, int to, long id, IntToLongFunction fv, LongBinaryOperator fm) {
		long ret = id;
		for (int i=from; i<to; i++) {
			ret = fm.applyAsLong(ret, fv.applyAsLong(i));
		}
		return ret;
	}

	private static void _execute(int from, int to, long id, IntToLongFunction fv, LongBinaryOperator fm, LongConsumer fh) {
		if (from>=to) {
			fh.accept(id);
		} else if (from+1==to) {
			fh.accept(fv.applyAsLong(from));
		} else if (xs.fork(to-from)) {
			int h = (from+to)>>1;
			xs.queue((LongConsumer nh) -> _execute(from, h, id, fv, fm, nh),
					(LongConsumer nh) -> _execute(h, to, id, fv, fm, nh),
					fh, fm);
		} else {
			fh.accept(_execute(from, to, id, fv, fm));
		}
	}
	
	/**
	 * (P) Executes a lambda for all arguments within range (in undefined order)
	 * and combines the result with an associative operator.
	 * 
	 * @param from Start of the execution range (inclusive).
	 * @param to End of the execution range (exclusive).
	 * @param id Identity element (to be returned if <code>from>=to</code>).
	 * @param fv Lambda to executed (with all arguments within the execution range).
	 * @param fm Associative operator (for merging results).
	 * @return Result of the operation.
	 * @since 1.0.0
	 */
	public static <T> T execute(int from, int to, T id, IntFunction<T> fv, BinaryOperator<T> fm) {
		return xs.fork(to-from)
				? xs.executeAsObj((Consumer<T> fh) -> _execute(from, to, id, fv, fm, fh))
				: _execute(from, to, id, fv, fm);
	}
	
	@SuppressWarnings("unchecked")
	private static <T> T _execute(int from, int to, T id, IntFunction<T> fv, BinaryOperator<T> fm) {
		if (from>=to) {
			return id;
		}
		Object[] rt = new Object[to-from];
		for (int i=0; i<rt.length; i++) {
			rt[i] = fv.apply(from+i);
		}
		int ln = rt.length;
		while (ln>1) {
			int nln = 0;
			for (int i=0; i+1<ln; i+=2) {
				rt[nln++] = fm.apply((T)rt[i], (T)rt[i+1]);
			}
			if ((ln&1)==1) {
				rt[nln++] = rt[ln-1];
			}
			ln = nln;
		}		
		return (T) rt[0];
	}

	private static <T> void _execute(int from, int to, T id, IntFunction<T> fv, BinaryOperator<T> fm, Consumer<T> fh) {
		if (from>=to) {
			fh.accept(id);
		} else if (from+1==to) {
			fh.accept(fv.apply(from));
		} else if (xs.fork(to-from)) {
			int h = (from+to)>>1;
			xs.queue((Consumer<T> nh) -> _execute(from, h, id, fv, fm, nh),
					(Consumer<T> nh) -> _execute(h, to, id, fv, fm, nh),
					fh, fm);
		} else {
			fh.accept(_execute(from, to, id, fv, fm));
		}
	}
	
	/* POPULATE */

	/**
	 * (P) Creates and populates an array of <code>int</code>s.
	 * 
	 * @param ln Length of the array to be created.
	 * @param fn Supplies the nth element of the array.
	 * @return An array.
	 * @since 1.0.0
	 */
	public static int[] populate(int ln, IntUnaryOperator fn) {
		int[] src = new int[ln];
		execute(0, src.length, (int p) -> { src[p] = fn.applyAsInt(p); });
		return src;
	}
	
	/**
	 * (P) Creates and populates an array of <code>long</code>s.
	 * 
	 * @param ln Length of the array to be created.
	 * @param fn Supplies the nth element of the array.
	 * @return An array.
	 * @since 1.0.0
	 */
	public static long[] populate(int ln, IntToLongFunction fn) {
		long[] src = new long[ln];
		execute(0, src.length, (int p) -> { src[p] = fn.applyAsLong(p); });
		return src;
	}
	
	/**
	 * (P) Creates and populates an array of objects.
	 *
	 * @param <T> Item type.
	 * @param rc Item type.
	 * @param ln Length of the array to be created.
	 * @param fn Supplies the nth element of the array.
	 * @return An array.
	 * @since 1.0.0
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] populate(Class<? extends T> rc, int ln, IntFunction<T> fn) {
		T[] src = (T[]) Array.newInstance(rc, ln);
		execute(0, src.length, (int p) -> { src[p] = fn.apply(p); });
		return src;
	}
	
	/**
	 * (P) Creates and populates a 2-dimensional array of <code>int</code>s.
	 * 
	 * @param lno Outer length of the array to be created.
	 * @param lni Inner length of the array to be created.
	 * @param fn Supplies the nth element of the array.
	 * @return An array.
	 * @since 1.0.0
	 */
	public static int[][] populate(int lno, int lni, IntBinaryOperator fn) {
		return populate(int[].class, lno, (int p) -> populate(lni, (int q) -> fn.applyAsInt(p, q)));
	}
	
	/**
	 * (P) Creates and populates a 2-dimensional array of <code>long</code>s.
	 * 
	 * @param lno Outer length of the array to be created.
	 * @param lni Inner length of the array to be created.
	 * @param fn Supplies the nth element of the array.
	 * @return An array.
	 * @since 1.0.0
	 */
	public static long[][] populate(int lno, int lni, IntToLongBinaryOperator fn) {
		return populate(long[].class, lno, (int p) -> populate(lni, (int q) -> fn.applyAsLong(p, q)));
	}
	
	/**
	 * (P) Creates and populates a 2-dimensional array of objects.
	 * 
	 * @param <T> Item type.
	 * @param rc Item type.
	 * @param lno Outer length of the array to be created.
	 * @param lni Inner length of the array to be created.
	 * @param fn Supplies the nth element of the array.
	 * @return An array.
	 * @since 1.0.0
	 */
	public static <T> T[][] populate(Class<? extends T> rc, int lno, int lni, IntBiFunction<T> fn) {
		return populate(wrap(rc), lno, (int p) -> populate(rc, lni, (int q) -> fn.apply(p, q)));
	}

	
	/* JOIN */

	/**
	 * (P) Concatenates a sequence of arrays of <code>int</code>s.
	 * 
	 * @param ln Number of arrays to concatenate.
	 * @param fn Supplies the nth array.
	 * @return The combined array.
	 * @since 1.0.0
	 */
	public static int[] joinAsInt(int ln, IntFunction<int[]> fn) {
		int[][] rt = populate(int[].class, ln, fn);
		int[] lns = new int[rt.length];
		int nln = 0;
		for (int i=0; i<rt.length; i++) {
			lns[i] = nln;
			nln += rt[i].length;
		}
		if (nln==0) {
			return EMPTY_INT;
		}
		int[] ret = new int[nln];
		execute(0, rt.length, (int p) -> System.arraycopy(rt[p], 0, ret, lns[p], rt[p].length));
		return ret;
	}

	/**
	 * (P) Concatenates a sequence of arrays of <code>long</code>s.
	 * 
	 * @param ln Number of arrays to concatenate.
	 * @param fn Supplies the nth array.
	 * @return The combined array.
	 * @since 1.0.0
	 */
	public static long[] joinAsLong(int ln, IntFunction<long[]> fn) {
		long[][] rt = populate(long[].class, ln, fn);
		int[] lns = new int[rt.length];
		int nln = 0;
		for (int i=0; i<rt.length; i++) {
			lns[i] = nln;
			nln += rt[i].length;
		}
		if (nln==0) {
			return EMPTY_LONG;
		}
		long[] ret = new long[nln];
		execute(0, rt.length, (int p) -> System.arraycopy(rt[p], 0, ret, lns[p], rt[p].length));
		return ret;
	}

	/**
	 * (P) Concatenates a sequence of arrays of objects.
	 * 
	 * @param <T> Item type.
	 * @param rc Item type.
	 * @param ln Number of arrays to concatenate.
	 * @param fn Supplies the nth array.
	 * @return The combined array.
	 * @since 1.0.0
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] join(Class<? extends T> rc, int ln, IntFunction<T[]> fn) {
		T[][] rt = populate(wrap(rc), ln, fn);
		int[] lns = new int[rt.length];
		int nln = 0;
		for (int i=0; i<rt.length; i++) {
			lns[i] = nln;
			nln += rt[i].length;
		}
		T[] ret = (T[]) Array.newInstance(rc, nln);
		execute(0, rt.length, (int p) -> System.arraycopy(rt[p], 0, ret, lns[p], rt[p].length));
		return ret;
	}
	
	/**
	 * Concatenates arrays of <code>int</code>s.
	 * @param as The left array.
	 * @param bs The right array.
	 * @return An array.
	 * @since 1.0.0
	 */
	public static int[] join(int[] as, int[] bs) {
		int[] rt = Arrays.copyOf(as, as.length+bs.length);
		System.arraycopy(bs, 0, rt, as.length, bs.length);
		return rt;
	}
	
	/**
	 * Concatenates arrays of <code>long</code>s.
	 * @param as The left array.
	 * @param bs The right array.
	 * @return An array.
	 * @since 1.0.0
	 */
	public static long[] join(long[] as, long[] bs) {
		long[] rt = Arrays.copyOf(as, as.length+bs.length);
		System.arraycopy(bs, 0, rt, as.length, bs.length);
		return rt;
	}
	
	/**
	 * Concatenates arrays of objects.
	 * @param as The left array.
	 * @param bs The right array.
	 * @return An array.
	 * @since 1.0.0
	 */
	public static <T> T[] join(T[] as, T[] bs) {
		T[] rt = Arrays.copyOf(as, as.length+bs.length);
		System.arraycopy(bs, 0, rt, as.length, bs.length);
		return rt;
	}

	/**
	 * Concatenates arrays of <code>int</code>s.
	 * @param as The left array.
	 * @param bs The middle array.
	 * @param cs The right array.
	 * @return An array.
	 * @since 1.0.0
	 */
	public static int[] join(int[] as, int[] bs, int[] cs) {
		int[] rt = Arrays.copyOf(as, as.length+bs.length+cs.length);
		System.arraycopy(bs, 0, rt, as.length, bs.length);
		System.arraycopy(cs, 0, rt, as.length+bs.length, cs.length);
		return rt;
	}

	/**
	 * Concatenates arrays of <code>long</code>s.
	 * @param as The left array.
	 * @param bs The middle array.
	 * @param cs The right array.
	 * @return An array.
	 * @since 1.0.0
	 */
	public static long[] join(long[] as, long[] bs, long[] cs) {
		long[] rt = Arrays.copyOf(as, as.length+bs.length+cs.length);
		System.arraycopy(bs, 0, rt, as.length, bs.length);
		System.arraycopy(cs, 0, rt, as.length+bs.length, cs.length);
		return rt;
	}

	/**
	 * Concatenates arrays of objects.
	 * @param as The left array.
	 * @param bs The middle array.
	 * @param cs The right array.
	 * @return An array.
	 * @since 1.0.0
	 */
	public static <T> T[] join(T[] as, T[] bs, T[] cs) {
		T[] rt = Arrays.copyOf(as, as.length+bs.length+cs.length);
		System.arraycopy(bs, 0, rt, as.length, bs.length);
		System.arraycopy(cs, 0, rt, as.length+bs.length, cs.length);
		return rt;
	}	

	/* SORT/A */

	/**
	 * (P) Computes absolute order for/sorts a number of <code>int</code>s; equivalent items are
	 * merged using the supplied function.
	 * 
	 * @param ln Number of items to be sorted.
	 * @param fv Supplies the nth item to be sorted.
	 * @param cmp Comparator function.
	 * @param fm Merge function (for equivalent items; may be null if there are no equivalent items).
	 * @return The sorted array.
	 * @since 1.0.0
	 */
	public static int[] order(int ln, IntUnaryOperator fv, IntBinaryOperator cmp, IntBinaryOperator fm) {
		return execute(0, ln, EMPTY_INT,
				(int p) -> new int[] { fv.applyAsInt(p) },
				(int[] ra, int[] rb) -> merge(ra, rb, cmp, fm, SetOperator.UNION));
	}
	
	/**
	 * (P) Computes absolute order for/sorts a number of <code>longs</code>s; equivalent items are
	 * merged using the supplied function.
	 * 
	 * @param ln Number of items to be sorted.
	 * @param fv Supplies the nth item to be sorted.
	 * @param cmp Comparator function.
	 * @param fm Merge function (for equivalent items; may be null if there are no equivalent items).
	 * @return The sorted array.
	 * @since 1.0.0
	 */
	public static long[] order(int ln, IntToLongFunction fv, LongToIntBinaryOperator cmp, LongBinaryOperator fm) {
		return execute(0, ln, EMPTY_LONG,
				(int p) -> new long[] { fv.applyAsLong(p) },
				(long[] ra, long[] rb) -> merge(ra, rb, cmp, fm, SetOperator.UNION));
	}
	
	/**
	 * (P) Computes absolute order for/sorts a number of objects; equivalent items are
	 * merged using the supplied function.
	 * 
	 * @param <T> Item type.
	 * @param rc Item type.
	 * @param ln Number of items to be sorted.
	 * @param fv Supplies the nth item to be sorted.
	 * @param cmp Comparator function.
	 * @param fm Merge function (for equivalent items; may be null if there are no equivalent items).
	 * @return The sorted array.
	 * @since 1.0.0
	 */
	public static <T> T[] order(Class<? extends T> rc, int ln, IntFunction<T> fv, Comparator<T> cmp, BinaryOperator<T> fm) {
		return execute(0, ln, empty(rc),
				(int p) -> wrap(fv.apply(p)),
				(T[] ra, T[] rb) -> merge(rc, ra, rb, cmp, fm, SetOperator.UNION));
	}
	
	/* SORT/R */

	/**
	 * (P) Computes absolute order for/sorts a number of <code>int</code>s; equivalent items are
	 * grouped into arrays.
	 * 
	 * @param ln Number of items to be sorted.
	 * @param fv Supplies the nth item to be sorted.
	 * @param ocmp Outer comparator (for splitting).
	 * @param icmp Inner comparator (for equivalent items; if null, the order between equivalent items is preserved). 
	 * @return The sorted array of arrays.
	 * @since 1.0.0
	 */
	public static int[][] groups(int ln, IntUnaryOperator fv, IntBinaryOperator ocmp, IntBinaryOperator icmp) {
		return order(int[].class, ln,
				(int p) -> new int[] { fv.applyAsInt(p) },
				(int[] a, int[] b) -> ocmp.applyAsInt(a[0], b[0]),
				icmp==null ? RJ::join : (int[] a, int[] b) -> merge(a, b, icmp, (int u, int v) -> u, SetOperator.UNION));
	}
	
	/**
	 * (P) Computes absolute order for/sorts a number of <code>long</code>s; equivalent items are
	 * grouped into arrays.
	 * 
	 * @param ln Number of items to be sorted.
	 * @param fv Supplies the nth item to be sorted.
	 * @param ocmp Outer comparator (for splitting).
	 * @param icmp Inner comparator (for equivalent items; if null, the order between equivalent items is preserved). 
	 * @return The sorted array of arrays.
	 * @since 1.0.0
	 */
	public static long[][] groups(int ln, IntToLongFunction fv, LongToIntBinaryOperator ocmp, LongToIntBinaryOperator icmp) {
		return order(long[].class, ln,
				(int p) -> new long[] { fv.applyAsLong(p) },
				(long[] a, long[] b) -> ocmp.applyAsInt(a[0], b[0]),
				icmp==null ? RJ::join : (long[] a, long[] b) -> merge(a, b, icmp, (long u, long v) -> u, SetOperator.UNION));
	}
	
	/**
	 * (P) Computes absolute order for/sorts a number of objects; equivalent items are
	 * grouped into arrays.
	 * 
	 * @param <T> Item type.
	 * @param rc Item type.
	 * @param ln Number of items to be sorted.
	 * @param fv Supplies the nth item to be sorted.
	 * @param ocmp Outer comparator (for splitting).
	 * @param icmp Inner comparator (for equivalent items; if null, the order between equivalent items is preserved). 
	 * @return The sorted array of arrays.
	 * @since 1.0.0
	 */
	public static <T> T[][] groups(Class<? extends T> rc, int ln, IntFunction<T> fv, Comparator<T> ocmp, Comparator<T> icmp) {
		return order(wrap(rc), ln,
				(int p) -> wrap(fv.apply(p)),
				(T[] a, T[] b) -> ocmp.compare(a[0], b[0]),
				icmp==null ? RJ::join : (T[] a, T[] b) -> merge(rc, a, b, icmp, (T u, T v) -> u, SetOperator.UNION));
	}

	/* ORDER/A */

	/**
	 * (P) Computes absolute order on a preorder of <code>int</code>s.
	 * 
	 * @param src An array of arrays.
	 * @param ocmp Outer comparator (for splitting).
	 * @param icmp Inner comparator (for equivalent items; if null, the order between equivalent items is preserved).
	 * @return The sorted array of arrays.
	 * @since 1.0.0
	 */
	public static int[][] groups(int[][] src, IntBinaryOperator ocmp, IntBinaryOperator icmp) {
		int[][] ret = join(int[].class, src.length, (int p) -> {
			if (src[p].length<=1) {
				return new int[][] { src[p] };
			}
			int[][] rt = groups(src[p].length, (int q) -> src[p][q], ocmp, icmp);
			return rt.length==1 ? new int[][] { src[p] } : rt;
		});
		return ret.length==src.length ? src : ret;
	}
	
	/**
	 * (P) Computes absolute order on a preorder of <code>long</code>s.
	 * 
	 * @param src An array of arrays.
	 * @param ocmp Outer comparator (for splitting).
	 * @param icmp Inner comparator (for equivalent items; if null, the order between equivalent items is preserved).
	 * @return The sorted array of arrays.
	 * @since 1.0.0
	 */
	public static long[][] groups(long[][] src, LongToIntBinaryOperator ocmp, LongToIntBinaryOperator icmp) {
		long[][] ret = join(long[].class, src.length, (int p) -> {
			if (src[p].length<=1) {
				return new long[][] { src[p] };
			}
			long[][] rt = groups(src[p].length, (int q) -> src[p][q], ocmp, icmp);
			return rt.length==1 ? new long[][] { src[p] } : rt;
		});
		return ret.length==src.length ? src : ret;
	}
	
	/**
	 * (P) Computes absolute order on a preorder of objects.
	 * 
	 * @param <T> Item type.
	 * @param rc Item type.
	 * @param src An array of arrays.
	 * @param ocmp Outer comparator (for splitting).
	 * @param icmp Inner comparator (for equivalent items; if null, the order between equivalent items is preserved).
	 * @return The sorted array of arrays.
	 * @since 1.0.0
	 */
	public static <T> T[][] groups(Class<? extends T> rc, T[][] src, Comparator<T> ocmp, Comparator<T> icmp) {
		T[][] ret = join(wrap(rc), src.length, (int p) -> {
			if (src[p].length<=1) {
				return wrap(src[p]);
			}
			T[][] rt = groups(rc, src[p].length, (int q) -> src[p][q], ocmp, icmp);
			return rt.length==1 ? wrap(src[p]) : rt;
		});
		return ret.length==src.length ? src : ret;
	}
	
	/* PROPAGATE/R */

	/**
	 * (P) Executes the iterative propagation part of relative order/comparision for a preorder
	 * of symbols expressed as <code>int</code>s.
	 * 
	 * @param src A preorder of symbols.
	 * @param fg Supplies the relative difference between a pair of symbols.
	 * @param cmp Comparator for equivalent items; if null, the order between equivalent items is preserved.
	 * @param g Group operator.
	 * @param fh Halting condition; if null, iteration is repeated as long as differences are found.
	 * @return An order of symbols.
	 * @since 1.0.0
	 */
	public static int[][] propagate(int[][] src, IntBinaryOperator fg, IntBinaryOperator cmp, IntGroupOperator g, Predicate<int[][]> fh) {
		while (fh==null || fh.test(src)) {
			int[][] _src = src;
			int[][] nsrc = groups(src, (int a, int b) -> {
				for (int i=_src.length-1; i>=0; i--) {
					int _i = i;
					int rt = g.signum(execute(0, _src[i].length, g.identityAsInt(), (int p) -> {
						int c = _src[_i][p];
						return g.cancelAsInt(fg.applyAsInt(a, c), fg.applyAsInt(b, c));
					}, g::applyAsInt));
					if (rt!=0) {
						return rt;
					}
				}
				return 0;
			}, cmp);
			if (nsrc.length==src.length) {
				break;
			}
			src = nsrc;
		}
		return src;
	}

	/**
	 * (P) Executes the iterative propagation part of relative order/comparision for a preorder
	 * of symbols expressed as <code>long</code>s.
	 * 
	 * @param src A preorder of symbols.
	 * @param fg Supplies the relative difference between a pair of symbols.
	 * @param cmp Comparator for equivalent items; if null, the order between equivalent items is preserved.
	 * @param g Group operator.
	 * @param fh Halting condition; if null, iteration is repeated as long as differences are found.
	 * @return An order of symbols.
	 * @since 1.0.0
	 */
	public static long[][] propagate(long[][] src, LongBinaryOperator fg, LongToIntBinaryOperator cmp, LongGroupOperator g, Predicate<long[][]> fh) {
		while (fh==null || fh.test(src)) {
			long[][] _src = src;
			long[][] nsrc = groups(src, (long a, long b) -> {
				for (int i=_src.length-1; i>=0; i--) {
					int _i = i;
					int rt = g.signum(execute(0, _src[i].length, g.identityAsLong(), (int p) -> {
						long c = _src[_i][p];
						return g.cancelAsLong(fg.applyAsLong(a, c), fg.applyAsLong(b, c));
					}, g::applyAsLong));
					if (rt!=0) {
						return rt;
					}
				}
				return 0;
			}, cmp);
			if (nsrc.length==src.length) {
				break;
			}
			src = nsrc;
		}
		return src;
	}
	
	/**
	 * (P) Executes the iterative propagation part of relative order/comparision for a preorder
	 * of symbols expressed as <code>long</code>s.
	 * 
	 * If no order is done, input order is returned as is.
	 * 
	 * @param <T> Symbol type.
	 * @param <E> Element type.
	 * @param rc Symbol type.
	 * @param src A preorder of symbols.
	 * @param fg Supplies the relative difference between a pair of symbols.
	 * @param cmp Comparator for equivalent items; if null, the order between equivalent items is preserved.
	 * @param g Group operator.
	 * @param fh Halting condition; if null, iteration is repeated as long as differences are found.
	 * @return An order of symbols.
	 * @since 1.0.0
	 */
	public static <T,E> T[][] propagate(Class<? extends T> rc, T[][] src, BiFunction<T,T,E> fg, Comparator<T> cmp, GroupOperator<E> g, Predicate<T[][]> fh) {
		E id = g.identity();
		while (fh==null || fh.test(src)) {
			T[][] _src = src;
			T[][] nsrc = groups(rc, src, (T a, T b) -> {
				for (int i=_src.length-1; i>=0; i--) {
					int _i = i;
					int rt = g.signum(execute(0, _src[i].length, id, (int p) -> {
						T c = _src[_i][p];
						return g.cancel(fg.apply(a, c), fg.apply(b, c));
					}, g::apply));
					if (rt!=0) {
						return rt;
					}
				}
				return 0;
			}, cmp);
			if (nsrc.length==src.length) {
				break;
			}
			src = nsrc;
		}
		return src;
	}
	
	/* COMPARE/R */
	
	/**
	 * (P) Computes the relative order between a pair of symbols expressed as <code>int</code>s.
	 * Returns -1, 0 or 1, depending whether left symbol is less than (absolute),
	 * equivalent to (neutral) or more than (relative) the right symbol.
	 * 
	 * If no order is done, input order is returned as is.
	 *  
	 * @param a Left symbol.
	 * @param b Right symbol.
	 * @param src A preorder of symbols.
	 * @param fg Supplies the relative difference between a pair of symbols.
	 * @param g Group operator.
	 * @return Result of comparision (-1, 0 or 1).
	 * @since 1.0.0
	 */
	public static int compare(int a, int b, int[][] src, IntBinaryOperator fg, IntGroupOperator g) {
		if (a==b) {
			return 0;
		}
		
		int[][] rt = propagate(populate(int[].class, src.length+1, (int p) -> {
					if (p==0) {
						return new int[] { a<<1, (b<<1)|1 };
					} else {
						int[] ks = src[p-1];
						int[] rs = new int[ks.length<<1];
						int q=0;
						for (int i=0; i<ks.length; i++) {
							if (ks[i]!=a) {
								rs[q++] = ks[i]<<1;
							}
							if (ks[i]!=b) {
								rs[q++] = (ks[i]<<1)|1;
							}
						}
						return q==rs.length ? rs : Arrays.copyOf(rs, q);
					}
				}),
				(int u, int v) -> (u&1)==(v&1)
					? fg.applyAsInt(u>>1, v>>1)
					: g.identityAsInt(),
				null, g,
				(int[][] s) -> s[0].length==2);
		
		return rt[0].length==2 ? 0 : rt[0][0]==a<<1 ? -1 : 1;
	}
	
	/**
	 * (P) Computes the relative order between a pair of symbols expressed as <code>long</code>s.
	 * Returns -1, 0 or 1, depending whether left symbol is less than (absolute),
	 * equivalent to (neutral) or more than (relative) the right symbol.
	 * 
	 * If no order is done, input order is returned as is.
	 *  
	 * @param a Left symbol.
	 * @param b Right symbol.
	 * @param src A preorder of symbols.
	 * @param fg Supplies the relative difference between a pair of symbols.
	 * @param g Group operator.
	 * @return Result of comparision (-1, 0 or 1).
	 * @since 1.0.0
	 */
	public static int compare(long a, long b, long[][] src, LongBinaryOperator fg, LongGroupOperator g) {
		if (a==b) {
			return 0;
		}
		
		long[][] rt = propagate(populate(long[].class, src.length+1, (int p) -> {
					if (p==0) {
						return new long[] { a<<1, (b<<1)|1 };
					} else {
						long[] ks = src[p-1];
						long[] rs = new long[ks.length<<1];
						int q=0;
						for (int i=0; i<ks.length; i++) {
							if (ks[i]!=a) {
								rs[q++] = ks[i]<<1;
							}
							if (ks[i]!=b) {
								rs[q++] = (ks[i]<<1)|1;
							}
						}
						return q==rs.length ? rs : Arrays.copyOf(rs, q);
					}
				}),
				(long u, long v) -> (u&1)==(v&1)
					? fg.applyAsLong(u>>1, v>>1)
					: g.identityAsLong(),
				null, g,
				(long[][] s) -> s[0].length==2);
		
		return rt[0].length==2 ? 0 : rt[0][0]==a<<1 ? -1 : 1;
	}

	/**
	 * (P) Computes the relative order between a pair of symbols expressed as objects.
	 * Returns -1, 0 or 1, depending whether left symbol is less than (absolute),
	 * equivalent to (neutral) or more than (relative) the right symbol.
	 *  
	 * @param <T> Symbol type.
	 * @param <E> Element type.
	 * @param rc Symbol type.
	 * @param a Left symbol.
	 * @param b Right symbol.
	 * @param src A preorder of symbols.
	 * @param fg Supplies the relative difference between a pair of symbols.
	 * @param g Group operator.
	 * @return Result of comparision (-1, 0 or 1).
	 * @since 1.0.0
	 */
	@SuppressWarnings("unchecked")
	public static <T,E> int compare(Class<? extends T> rc, T a, T b, T[][] src, BiFunction<T,T,E> fg, GroupOperator<E> g) {
		if (Objects.equals(a,b)) {
			return 0;
		}
		
		BPrefix<T>[][] rt = propagate((Class<? extends BPrefix<T>>)BPrefix.class.asSubclass( BPrefix.class), (BPrefix<T>[][]) populate(BPrefix[].class, src.length+1, (int p) -> {
					if (p==0) {
						return new BPrefix[] { new BPrefix<>(false, a), new BPrefix<>(true, b) };
					} else {
						T[] ks = src[p-1];
						BPrefix<T>[] rs = new BPrefix[ks.length<<1];
						int q=0;
						for (int i=0; i<ks.length; i++) {
							if (ks[i]!=a) {
								rs[q++] = new BPrefix<>(false, ks[i]);
							}
							if (ks[i]!=b) {
								rs[q++] = new BPrefix<>(true, ks[i]);
							}
						}
						return q==rs.length ? rs : Arrays.copyOf(rs, q);
					}
				}),
				(BPrefix<T> u, BPrefix<T> v) -> u.b==v.b
					? fg.apply(u.v, v.v)
					: g.identity(),
				null, g,
				(BPrefix<T>[][] s) -> s[0].length==2);
		
		return rt[0].length==2 ? 0 : rt[0][0].v==a ? -1 : 1;
	}

	private static class BPrefix<T> {
		
		final boolean b;
		final T v;
		
		BPrefix(boolean b, T v) {
			super();
			this.b = b;
			this.v = v;
		}
				
	}
	
	/* GROUPS/R */

	/**
	 * (P) Computes a (truly) relative (pre)order of automorphic groups of a preorder of symbols
	 * expressed as an array of arrays of <code>int</code>s.
	 * 
	 * If no order is done, input order is returned as is.
	 * 
	 * @param src A preorder of symbols.
	 * @param fg Supplies the relative difference between a pair of symbols.
	 * @param g Group operator.
	 * @param cmp Comparator for equivalent symbols; if null, the order between equivalent symbols is preserved.
	 * @return A preorder of automorphic groups of symbols.
	 * @since 1.0.0
	 */
	public static int[][] groups(int[][] src, IntBinaryOperator fg, IntGroupOperator g, IntBinaryOperator cmp) {
		int[][] _src = propagate(src, fg, null, g, null);
		int[][] ret = groups(_src, (int a, int b) -> compare(a,b, _src, fg, g), null);
		return cmp==null ? ret : populate(int[].class, ret.length, (int p) -> order(ret[p].length, (int q) -> ret[p][q], cmp, null));
	}
	
	/**
	 * (P) Computes a (truly) relative (pre)order of automorphic groups of a preorder of symbols
	 * expressed as an array of arrays of <code>long</code>s.
	 * 
	 * If no order is done, input order is returned as is.
	 * 
	 * @param src A preorder of symbols.
	 * @param fg Supplies the relative difference between a pair of symbols.
	 * @param g Group operator.
	 * @param cmp Comparator for equivalent symbols; if null, the order between equivalent symbols is preserved.
	 * @return A preorder of automorphic groups of symbols.
	 * @since 1.0.0
	 */
	public static long[][] groups(long[][] src, LongBinaryOperator fg, LongGroupOperator g, LongToIntBinaryOperator cmp) {
		long[][] _src = propagate(src, fg, null, g, null);
		long[][] ret = groups(_src, (long a, long b) -> compare(a,b, _src, fg, g), null);
		return cmp==null ? ret : populate(long[].class, ret.length, (int p) -> order(ret[p].length, (int q) -> ret[p][q], cmp, null));
	}
	
	/**
	 * (P) Computes a (truly) relative (pre)order of automorphic groups of a preorder of symbols
	 * expressed as an array of arrays of objects.
	 * 
	 * If no order is done, input order is returned as is.
	 * 
	 * @param <T> Symbol type.
	 * @param <E> Element type.
	 * @param rc Symbol type.
	 * @param src A preorder of symbols.
	 * @param fg Supplies the relative difference between a pair of symbols.
	 * @param g Group operator.
	 * @param cmp Comparator for equivalent symbols; if null, the order between equivalent symbols is preserved.
	 * @return A preorder of automorphic groups of symbols.
	 * @since 1.0.0
	 */
	public static <T,E> T[][] groups(Class<? extends T> rc, T[][] src, BiFunction<T,T,E> fg, GroupOperator<E> g, Comparator<T> cmp) {
		T[][] _src = propagate(rc, src, fg, null, g, null);
		T[][] ret = groups(rc, _src, (T a, T b) -> compare(rc, a,b, _src, fg, g), null);
		return cmp==null ? ret : populate(wrap(rc), ret.length, (int p) -> order(rc, ret[p].length, (int q) -> ret[p][q], cmp, null));
	}

	/* ORDER/R */

	/**
	 * (P) Computes a (strongly) relative (canonical) order of symbols expressed
	 * as an array of arrays of <code>int</code>s.
	 * 
	 * @param src A preorder of symbols.
	 * @param fg Supplies the relative difference between a pair of symbols.
	 * @param g Group operator.
	 * @param cmp Comparator for equivalent symbols and pivoting; if null, the order between equivalent symbols is preserved and used for pivoting.
	 * @return A canonical order of symbols.
	 * @since 1.0.0
	 */
	public static int[] order(int[][] src, IntBinaryOperator fg, IntGroupOperator g, IntBinaryOperator cmp) {
		int[] ret = EMPTY_INT;
		while (true) {
			src = groups(src, fg, g, null);
			int[][] _src = src;
			int pv = search(src.length, 0, (int p) -> _src[p].length>1);
			ret = join(populate(src.length-pv-1, (int p) -> _src[p+pv+1][0]), ret);
			if (pv==-1) {
				break;
			}
			
			int sp = cmp==null ? 0 : search(0, src[pv].length, (int nr, int or) -> cmp.applyAsInt(_src[pv][nr], _src[pv][or])>0);
			int[] ss = new int[src[pv].length-1];
			System.arraycopy(src[pv], 0, ss, 0, sp);
			System.arraycopy(src[pv], sp+1, ss, sp, src[pv].length-sp-1);
			
			int[][] po = propagate(new int[][] { ss, { src[pv][sp] } }, fg, null, g, null);
			int[][] nsrc = new int[po.length+pv][];
			System.arraycopy(src, 0, nsrc, 0, pv);
			System.arraycopy(po, 0, nsrc, pv, po.length);
			src = nsrc;
		}
		return ret;
	}

	/**
	 * (P) Computes a (strongly) relative (canonical) order of symbols expressed
	 * as an array of arrays of <code>long</code>s.
	 * 
	 * @param src A preorder of symbols.
	 * @param fg Supplies the relative difference between a pair of symbols.
	 * @param g Group operator.
	 * @param cmp Comparator for equivalent symbols and pivoting; if null, the order between equivalent symbols is preserved and used for pivoting.
	 * @return A canonical order of symbols.
	 * @since 1.0.0
	 */
	public static long[] order(long[][] src, LongBinaryOperator fg, LongGroupOperator g, LongToIntBinaryOperator cmp) {
		long[] ret = EMPTY_LONG;
		while (true) {
			src = groups(src, fg, g, null);
			long[][] _src = src;
			int pv = search(src.length, 0, (int p) -> _src[p].length>1);
			ret = join(populate(src.length-pv-1, (int p) -> _src[p+pv+1][0]), ret);
			if (pv==-1) {
				break;
			}

			int sp = cmp==null ? 0 : search(0, src[pv].length, (int nr, int or) -> cmp.applyAsInt(_src[pv][nr], _src[pv][or])>0);
			long[] ss = new long[src[pv].length-1];
			System.arraycopy(src[pv], 0, ss, 0, sp);
			System.arraycopy(src[pv], sp+1, ss, sp, src[pv].length-sp-1);
			
			long[][] po = propagate(new long[][] { ss, { src[pv][sp] } }, fg, null, g, null);
			long[][] nsrc = new long[po.length+pv][];
			System.arraycopy(src, 0, nsrc, 0, pv);
			System.arraycopy(po, 0, nsrc, pv, po.length);
			src = nsrc;
		}
		return ret;
	}

	/**
	 * (P) Computes a (strongly) relative (canonical) order of symbols expressed
	 * as an array of arrays of objects.
	 * 
	 * @param <T> Symbol type.
	 * @param <E> Element type.
	 * @param rc Symbol type.
	 * @param src A preorder of symbols.
	 * @param fg Supplies the relative difference between a pair of symbols.
	 * @param g Group operator.
	 * @param cmp Comparator for equivalent symbols and pivoting; if null, the order between equivalent symbols is preserved and used for pivoting.
	 * @return A canonical order of symbols.
	 * @since 1.0.0
	 */
	@SuppressWarnings("unchecked")
	public static <T,E> T[] order(Class<? extends T> rc, T[][] src, BiFunction<T,T,E> fg, GroupOperator<E> g, Comparator<T> cmp) {
		Class<? extends T[]> rca = wrap(rc);
		T[] ret = empty(rc);
		while (true) {
			src = groups(rc, src, fg, g, null);
			T[][] _src = src;
			int pv = search(src.length, 0, (int p) -> _src[p].length>1);
			ret = join(populate(rc, src.length-pv-1, (int p) -> _src[p+pv+1][0]), ret);
			if (pv==-1) {
				break;
			}
			
			int sp = cmp==null ? 0 : search(0, src[pv].length, (int nr, int or) -> cmp.compare(_src[pv][nr], _src[pv][or])>0);
			T[] ss = (T[]) Array.newInstance(rc, src[pv].length-1);
			System.arraycopy(src[pv], 0, ss, 0, sp);
			System.arraycopy(src[pv], sp+1, ss, sp, src[pv].length-sp-1);

			T[][] po = (T[][]) Array.newInstance(rca,2);
			po[0] = ss;
			po[1] = wrap(src[pv][sp]);

			po = propagate(rc, po, fg, null, g, null);
			T[][] nsrc = (T[][]) Array.newInstance(rca,po.length+pv);
			System.arraycopy(src, 0, nsrc, 0, pv);
			System.arraycopy(po, 0, nsrc, pv, po.length);
			src = nsrc;
		}
		return ret;
	}


	/* WRAP */

	/**
	 * Wraps an object with an array (intended for internal use).
	 * 
	 * @param <T> Item type.
	 * @param v Item.
	 * @return An array.
	 * @since 1.0.0
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] wrap(T v) {
		T[] rt = (T[]) Array.newInstance(v.getClass(), 1);
		rt[0] = v;
		return rt;
	}
	
	/**
	 * Wraps a type into an array type (intended for internal use).
	 * @param <T> Item type.
	 * @param rc Item type.
	 * @return An array type.
	 * @since 1.0.0
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<T[]> wrap(Class<? extends T> rc) {
		return (Class<T[]>) Array.newInstance(rc, 0).getClass();
	}
	
	/**
	 * Returns an empty array of the supplied type (intended for internal use).
	 * @param <T> Item type.
	 * @param rc Item type.
	 * @return An empty array.
	 * @since 1.0.0
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] empty(Class<? extends T> rc) {
		return (T[]) Array.newInstance(rc, 0);
	}

	/* MERGE/ANARY */

	/**
	 * (P) Applies an anary set operation to an array of sets of <code>int</code>s.
	 * 
	 * @param n Number of sets to merge.
	 * @param fn Supplies nth set to merge.
	 * @param cmp Comparator (both arguments must abide the order).
	 * @param fm Merge function (for combining equivalent items).
	 * @param op Symmetric set operation.
	 * @return The merged set.
	 * @since 1.0.0
	 */
	public static int[] merge(int n, IntFunction<int[]> fn, IntBinaryOperator cmp, IntBinaryOperator fm, SetOperator op) {
		op.requireSymmetric();
		return op==SetOperator.EMPTY
				? EMPTY_INT
				: execute(0, n, EMPTY_INT, fn, (int[] a, int[] b) -> merge(a, b, cmp, fm, op));
	}
	
	/**
	 * (P) Applies an anary set operation to an array of sets of <code>long</code>s.
	 * 
	 * @param n Number of sets to merge.
	 * @param fn Supplies nth set to merge.
	 * @param cmp Comparator (both arguments must abide the order).
	 * @param fm Merge function (for combining equivalent items).
	 * @param op Symmetric set operation.
	 * @return The merged set.
	 * @since 1.0.0
	 */
	public static long[] merge(int n, IntFunction<long[]> fn, LongToIntBinaryOperator cmp, LongBinaryOperator fm, SetOperator op) {
		op.requireSymmetric();
		return op==SetOperator.EMPTY
				? EMPTY_LONG
				: execute(0, n, EMPTY_LONG, fn, (long[] a, long[] b) -> merge(a, b, cmp, fm, op));
	}
	
	/**
	 * (P) Applies an anary set operation to an array of sets of objects.
	 * 
	 * <p>Note that if the item type is a generic type, the lambdas must work with the raw types.
	 * For example, assuming there is a generic class <code>SomeType&lt;T&gt;</code> which implements
	 * <code>Comparable&lt;SomeType&lt;T&gt;&gt;</code> and lambda <code>fn</code> is of type
	 * <code>IntFunction&lt;SomeType&lt;T&gt;&gt;</code>, then:
	 * </p>
	 * 
	 * <pre>
	 * @SuppressWarnings({ "unchecked", "rawtypes" })
	 * SomeType&lt;T&gt;[] ns = RJ.merge(SomeType.class, n,
	 * 		(int p) -> (SomeType[]) fn.apply(p),
	 * 		Comparator.naturalOrder(),
	 * 		(AbstractLNode a, AbstractLNode b) -> a,
	 * 		SetOperator.UNION);
	 * </pre>
	 * 
	 * @param <T> Item type.
	 * @param rc Item type.
	 * @param n Number of sets to merge.
	 * @param fn Supplies nth set to merge.
	 * @param cmp Comparator (both arguments must abide the order).
	 * @param fm Merge function (for combining equivalent items).
	 * @param op Symmetric set operation.
	 * @return The merged set.
	 * @since 1.0.0
	 */
	public static <T> T[] merge(Class<? extends T> rc, int n, IntFunction<T[]> fn, Comparator<T> cmp, BinaryOperator<T> fm, SetOperator op) {
		op.requireSymmetric();
		return op==SetOperator.EMPTY
				? empty(rc)
				: execute(0, n, empty(rc), fn, (T[] a, T[] b) -> merge(rc, a, b, cmp, fm, op));
	}
	
	/* MERGE/BINARY/MANY-MANY */
	
	/**
	 * (P) Applies a set operation upon a pair of sets expressed as ordered arrays of <code>int</code>s.
	 * 
	 * @param as Left set.
	 * @param bs Right set.
	 * @param cmp Comparator (both arguments must abide the order).
	 * @param fm Merge function (for combining equivalent items).
	 * @param op Set operation.
	 * @return Result set.
	 * @since 1.0.0
	 */
	public static int[] merge(int[] as, int[] bs, IntBinaryOperator cmp, IntBinaryOperator fm, SetOperator op) {
		return op==SetOperator.EMPTY ? EMPTY_INT
				: op==SetOperator.LEFT ? as
				: op==SetOperator.RIGHT ? bs
				: xs.fork(as.length+bs.length) ? xs.executeAsObj((Consumer<int[]> h) -> _merge(0, as.length, as, 0, bs.length, bs, cmp, fm, op, h))
				: _merge(0, as.length, as, 0, bs.length, bs, cmp, fm, op);
	}

	private static void _merge(int afrom, int ato, int[] as, int bfrom, int bto, int[] bs, IntBinaryOperator cmp, IntBinaryOperator fm, SetOperator op, Consumer<int[]> fh) {
		if (ato-afrom<=0) {
			fh.accept(op.right ? bfrom==0 && bto==bs.length ? bs : bto>bfrom ? Arrays.copyOfRange(bs,bfrom,bto) : EMPTY_INT : EMPTY_INT);
		} else if (bto-bfrom<=0) {
			fh.accept(op.left ? afrom==0 && ato==as.length ? as : ato>afrom ? Arrays.copyOfRange(as,afrom,ato) : EMPTY_INT : EMPTY_INT);
		} else if (ato-afrom>2 && bto-bfrom>2 && xs.fork(ato+bto-afrom-bfrom)) {
			int ap, bp;
			if (ato-afrom >= bto-bfrom) {
				ap = (ato-afrom)>>1;
				int _bp = RJ.search(bfrom, bto, (int p) -> cmp.applyAsInt(bs[p],as[ap]));
				if (_bp==~bfrom) {
					_merge(ap, ato, as, bfrom, bto, bs, cmp, fm, op,
							op.left ? (int[] r) -> fh.accept(join(Arrays.copyOfRange(as, afrom, ap), r)) : fh);
					return;
				} else if (_bp==~bto) {
					_merge(afrom, ap, as, bfrom, bto, bs, cmp, fm, op,
							op.right ? (int[] r) -> fh.accept(join(r, Arrays.copyOfRange(as, ap, ato))) : fh);
					return;
				}
				bp = _bp<0 ? ~_bp : _bp;
			} else {
				bp = (bto-bfrom)>>1;
				int _ap = RJ.search(afrom, ato, (int p) -> cmp.applyAsInt(as[p],bs[bp]));
				if (_ap==~afrom) {
					_merge(afrom, ato, as, bp, bto, bs, cmp, fm, op,
							op.left ? (int[] r) -> fh.accept(join(Arrays.copyOfRange(bs, bfrom, bp), r)) : fh);
					return;
				} else if (_ap==~ato) {
					_merge(afrom, ato, as, bfrom, bp, bs, cmp, fm, op,
							op.right ? (int[] r) -> fh.accept(join(r,Arrays.copyOfRange(bs, bp, bto))) : fh);
					return;
				}
				ap = _ap<0 ? ~_ap : _ap;
			}
			xs.queue((Consumer<int[]> h) -> _merge(afrom, ap, as, bfrom, bp, bs, cmp, fm, op, h),
					(Consumer<int[]> h) -> _merge(ap, ato, as, bp, bto, bs, cmp, fm, op, h),
					fh, RJ::join);
		} else {
			fh.accept(_merge(afrom, ato, as, bfrom, bto, bs, cmp, fm, op));
		}		
	}

	private static int[] _merge(int afrom, int ato, int[] as, int bfrom, int bto, int[] bs, IntBinaryOperator cmp, IntBinaryOperator fm, SetOperator op) {
		int ln = ato+bto-afrom-bfrom;
		if (ln<=0) {
			return EMPTY_INT;
		}
		int[] rt = new int[ln];
		int ap=afrom, bp=bfrom, p=0;
		while (ap<ato && bp<bto) {
			int cv = cmp.applyAsInt(as[ap], bs[bp]);
			if (cv<0) {
				if (op.left) {
					rt[p++] = as[ap];
				}
				ap++;
			} else if (cv>0) {
				if (op.right) {
					rt[p++] = bs[bp];
				}
				bp++;
			} else {
				if (op.both) {
					rt[p++] = fm.applyAsInt(as[ap], bs[bp]);
				}
				ap++;
				bp++;
			}
		}
		if (op.left) {
			System.arraycopy(as, ap, rt, p, ato-ap);
			p += ato-ap;
		}		
		if (op.right) {
			System.arraycopy(bs, bp, rt, p, bto-bp);
			p += bto-bp;
		}
		return p==rt.length ? rt : Arrays.copyOf(rt, p);
	}
	
	/**
	 * (P) Applies a set operation upon a pair of sets expressed as ordered arrays of <code>long</code>s.
	 * 
	 * @param as Left set.
	 * @param bs Right set.
	 * @param cmp Comparator (both arguments must abide the order).
	 * @param fm Merge function (for combining equivalent items).
	 * @param op Set operation.
	 * @return Result set.
	 * @since 1.0.0
	 */
	public static long[] merge(long[] as, long[] bs, LongToIntBinaryOperator cmp, LongBinaryOperator fm, SetOperator op) {
		return op==SetOperator.EMPTY ? EMPTY_LONG
				: op==SetOperator.LEFT ? as
				: op==SetOperator.RIGHT ? bs
				: xs.fork(as.length+bs.length) ? xs.executeAsObj((Consumer<long[]> h) -> _merge(0, as.length, as, 0, bs.length, bs, cmp, fm, op, h))
				: _merge(0, as.length, as, 0, bs.length, bs, cmp, fm, op);
	}

	private static void _merge(int afrom, int ato, long[] as, int bfrom, int bto, long[] bs, LongToIntBinaryOperator cmp, LongBinaryOperator fm, SetOperator op, Consumer<long[]> fh) {
		if (ato-afrom<=0) {
			fh.accept(op.right ? bfrom==0 && bto==bs.length ? bs : bto>bfrom ? Arrays.copyOfRange(bs,bfrom,bto) : EMPTY_LONG : EMPTY_LONG);
		} else if (bto-bfrom<=0) {
			fh.accept(op.left ? afrom==0 && ato==as.length ? as : ato>afrom ? Arrays.copyOfRange(as,afrom,ato) : EMPTY_LONG : EMPTY_LONG);
		} else if (ato-afrom>2 && bto-bfrom>2 && xs.fork(ato+bto-afrom-bfrom)) {
			int ap, bp;
			if (ato-afrom >= bto-bfrom) {
				ap = (ato-afrom)>>1;
				int _bp = RJ.search(bfrom, bto, (int p) -> cmp.applyAsInt(bs[p],as[ap]));
				if (_bp==~bfrom) {
					_merge(ap, ato, as, bfrom, bto, bs, cmp, fm, op,
							op.left ? (long[] r) -> fh.accept(join(Arrays.copyOfRange(as, afrom, ap), r)) : fh);
					return;
				} else if (_bp==~bto) {
					_merge(afrom, ap, as, bfrom, bto, bs, cmp, fm, op,
							op.right ? (long[] r) -> fh.accept(join(r, Arrays.copyOfRange(as, ap, ato))) : fh);
					return;
				}
				bp = _bp<0 ? ~_bp : _bp;
			} else {
				bp = (bto-bfrom)>>1;
				int _ap = RJ.search(afrom, ato, (int p) -> cmp.applyAsInt(as[p],bs[bp]));
				if (_ap==~afrom) {
					_merge(afrom, ato, as, bp, bto, bs, cmp, fm, op,
							op.left ? (long[] r) -> fh.accept(join(Arrays.copyOfRange(bs, bfrom, bp), r)) : fh);
					return;
				} else if (_ap==~ato) {
					_merge(afrom, ato, as, bfrom, bp, bs, cmp, fm, op,
							op.right ? (long[] r) -> fh.accept(join(r,Arrays.copyOfRange(bs, bp, bto))) : fh);
					return;
				}
				ap = _ap<0 ? ~_ap : _ap;
			}
			xs.queue((Consumer<long[]> h) -> _merge(afrom, ap, as, bfrom, bp, bs, cmp, fm, op, h),
					(Consumer<long[]> h) -> _merge(ap, ato, as, bp, bto, bs, cmp, fm, op, h),
					fh, RJ::join);
		} else {
			fh.accept(_merge(afrom, ato, as, bfrom, bto, bs, cmp, fm, op));
		}		
		/*} else {
			int ln = ato+bto-afrom-bfrom;
			if (ato-afrom>2 && bto-bfrom>2 && xs.fork(ln)) {
				int ah = (ato-afrom)>>1;
				int bh = (bto-bfrom)>>1;
				
				int pb = RJ.search(bfrom, bto, (int p) -> cmp.applyAsInt(bs[p],as[ah]));
				int pa = RJ.search(afrom, ato, (int p) -> cmp.applyAsInt(as[p],bs[bh]));

				if (pb<0) {
					pb = ~pb;
				}
				if (pa<0) {
					pa = ~pa;
				}
				
				int a1 = Math.min(ah, pa);
				int a2 = Math.max(ah, pa);
				
				int b1 = Math.min(bh, pb);
				int b2 = Math.max(bh, pb);

				xs.queue((Consumer<long[]> h) -> _merge(afrom, a1, as, bfrom, b1, bs, cmp, fm, op, h),
						(Consumer<long[]> h) -> _merge(a1, a2, as, b1, b2, bs, cmp, fm, op, h),
						(Consumer<long[]> h) -> _merge(a2, ato, as, b2, bto, bs, cmp, fm, op, h),
						fh, RJ::join);
			} else {
				fh.accept(_merge(afrom, ato, as, bfrom, bto, bs, cmp, fm, op));
			}
		}*/
	}

	private static long[] _merge(int afrom, int ato, long[] as, int bfrom, int bto, long[] bs, LongToIntBinaryOperator cmp, LongBinaryOperator fm, SetOperator op) {
		int ln = ato+bto-afrom-bfrom;
		if (ln<=0) {
			return EMPTY_LONG;
		}
		long[] rt = new long[ato+bto-afrom-bfrom];
		int ap=afrom, bp=bfrom, p=0;
		while (ap<ato && bp<bto) {
			int cv = cmp.applyAsInt(as[ap], bs[bp]);
			if (cv<0) {
				if (op.left) {
					rt[p++] = as[ap];
				}
				ap++;
			} else if (cv>0) {
				if (op.right) {
					rt[p++] = bs[bp];
				}
				bp++;
			} else {
				if (op.both) {
					rt[p++] = fm.applyAsLong(as[ap], bs[bp]);
				}
				ap++;
				bp++;
			}
		}
		if (op.left) {
			System.arraycopy(as, ap, rt, p, ato-ap);
			p += ato-ap;
		}		
		if (op.right) {
			System.arraycopy(bs, bp, rt, p, bto-bp);
			p += bto-bp;
		}
		return p==rt.length ? rt : Arrays.copyOf(rt, p);
	}
	
	/**
	 * (P) Applies a set operation upon a pair of sets expressed as ordered arrays of objects.
	 * 
	 * @param <T> Item type.
	 * @param rc Item type.
	 * @param as Left set.
	 * @param bs Right set.
	 * @param cmp Comparator (both arguments must abide the order).
	 * @param fm Merge function (for combining equivalent items).
	 * @param op Set operation.
	 * @return Result set.
	 * @since 1.0.0
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] merge(Class<? extends T> rc, T[] as, T[] bs, Comparator<T> cmp, BinaryOperator<T> fm, SetOperator op) {
		return op==SetOperator.EMPTY ? (T[])empty(as.getClass().getComponentType())
				: op==SetOperator.LEFT ? as
				: op==SetOperator.RIGHT ? bs
				: xs.fork(as.length+bs.length) ? xs.executeAsObj((Consumer<T[]> h) -> _merge(rc, 0, as.length, as, 0, bs.length, bs, cmp, fm, op, h))
				: _merge(rc, 0, as.length, as, 0, bs.length, bs, cmp, fm, op);
	}

	@SuppressWarnings("unchecked")
	private static <T> void _merge(Class<? extends T> rc, int afrom, int ato, T[] as, int bfrom, int bto, T[] bs, Comparator<T> cmp, BinaryOperator<T> fm, SetOperator op, Consumer<T[]> fh) {
		if (ato-afrom<=0) {
			fh.accept(op.right ? bfrom==0 && bto==bs.length ? bs : bto>bfrom ? Arrays.copyOfRange(bs,bfrom,bto) : (T[])empty(as.getClass().getComponentType()) : (T[])empty(as.getClass().getComponentType()));
		} else if (bto-bfrom<=0) {
			fh.accept(op.left ? afrom==0 && ato==as.length ? as : ato>afrom ? Arrays.copyOfRange(as,afrom,ato) : (T[])empty(as.getClass().getComponentType()) : (T[])empty(as.getClass().getComponentType()));
		} else if (ato-afrom>2 && bto-bfrom>2 && xs.fork(ato+bto-afrom-bfrom)) {
			int ap, bp;
			if (ato-afrom >= bto-bfrom) {
				ap = (ato-afrom)>>1;
				int _bp = RJ.search(bfrom, bto, (int p) -> cmp.compare(bs[p],as[ap]));
				if (_bp==~bfrom) {
					_merge(rc, ap, ato, as, bfrom, bto, bs, cmp, fm, op,
							op.left ? (T[] r) -> fh.accept(join(Arrays.copyOfRange(as, afrom, ap), r)) : fh);
					return;
				} else if (_bp==~bto) {
					_merge(rc, afrom, ap, as, bfrom, bto, bs, cmp, fm, op,
							op.right ? (T[] r) -> fh.accept(join(r, Arrays.copyOfRange(as, ap, ato))) : fh);
					return;
				}
				bp = _bp<0 ? ~_bp : _bp;
			} else {
				bp = (bto-bfrom)>>1;
				int _ap = RJ.search(afrom, ato, (int p) -> cmp.compare(as[p],bs[bp]));
				if (_ap==~afrom) {
					_merge(rc, afrom, ato, as, bp, bto, bs, cmp, fm, op,
							op.left ? (T[] r) -> fh.accept(join(Arrays.copyOfRange(bs, bfrom, bp), r)) : fh);
					return;
				} else if (_ap==~ato) {
					_merge(rc, afrom, ato, as, bfrom, bp, bs, cmp, fm, op,
							op.right ? (T[] r) -> fh.accept(join(r,Arrays.copyOfRange(bs, bp, bto))) : fh);
					return;
				}
				ap = _ap<0 ? ~_ap : _ap;
			}
			xs.queue((Consumer<T[]> h) -> _merge(rc, afrom, ap, as, bfrom, bp, bs, cmp, fm, op, h),
					(Consumer<T[]> h) -> _merge(rc, ap, ato, as, bp, bto, bs, cmp, fm, op, h),
					fh, RJ::join);
		} else {
			fh.accept(_merge(rc, afrom, ato, as, bfrom, bto, bs, cmp, fm, op));
		}		
/*		} else {
			int ln = ato+bto-afrom-bfrom;
			if (ato-afrom>2 && bto-bfrom>2 && xs.fork(ln)) {
				int ah = (ato-afrom)>>1;
				int bh = (bto-bfrom)>>1;
				
				int pb = RJ.search(bfrom, bto, (int p) -> cmp.compare(bs[p],as[ah]));
				int pa = RJ.search(afrom, ato, (int p) -> cmp.compare(as[p],bs[bh]));

				if (pb<0) {
					pb = ~pb;
				}
				if (pa<0) {
					pa = ~pa;
				}
				
				int a1 = Math.min(ah, pa);
				int a2 = Math.max(ah, pa);
				
				int b1 = Math.min(bh, pb);
				int b2 = Math.max(bh, pb);

				xs.queue((Consumer<T[]> h) -> _merge(rc, afrom, a1, as, bfrom, b1, bs, cmp, fm, op, h),
						(Consumer<T[]> h) -> _merge(rc, a1, a2, as, b1, b2, bs, cmp, fm, op, h),
						(Consumer<T[]> h) -> _merge(rc, a2, ato, as, b2, bto, bs, cmp, fm, op, h),
						fh, RJ::join);
			} else {
				fh.accept(_merge(rc, afrom, ato, as, bfrom, bto, bs, cmp, fm, op));
			}
		}*/
	}

	@SuppressWarnings("unchecked")
	private static <T> T[] _merge(Class<? extends T> rc, int afrom, int ato, T[] as, int bfrom, int bto, T[] bs, Comparator<T> cmp, BinaryOperator<T> fm, SetOperator op) {
		int ln = ato+bto-afrom-bfrom;
		if (ln<=0) {
			return empty(rc);
		}
		T[] rt = (T[]) Array.newInstance(rc, ato+bto-afrom-bfrom);
		int ap=afrom, bp=bfrom, p=0;
		while (ap<ato && bp<bto) {
			int cv = cmp.compare(as[ap], bs[bp]);
			if (cv<0) {
				if (op.left) {
					rt[p++] = as[ap];
				}
				ap++;
			} else if (cv>0) {
				if (op.right) {
					rt[p++] = bs[bp];
				}
				bp++;
			} else {
				if (op.both) {
					rt[p++] = fm.apply(as[ap], bs[bp]);
				}
				ap++;
				bp++;
			}
		}
		if (op.left) {
			System.arraycopy(as, ap, rt, p, ato-ap);
			p += ato-ap;
		}		
		if (op.right) {
			System.arraycopy(bs, bp, rt, p, bto-bp);
			p += bto-bp;
		}
		return p==rt.length ? rt : Arrays.copyOf(rt, p);
	}
	
	/* SHUFFLE (int, long) */
	
	/**
	 * Shuffles an input supplied as a lambda which is called for every pair of
	 * indices to be swapped. 
	 * 
	 * @param from Start of range (inclusive).
	 * @param to End of range (exclusive).
	 * @param fs Swap function.
	 * @param rnd Random number generator.
	 * @since 1.0.0
	 */
	public static void shuffle(int from, int to, IntBinaryConsumer fs, Random rnd) {
		for (int i=from; i<to; i++) {
			int p = rnd.nextInt(to-i);
			if (p!=0) {
				fs.accept(i, from+p);
			}						
		}		
	}
	
	/**
	 * Shuffles an array of <code>int</code>s.
	 * 
	 * @param vs Array to be shuffled.
	 * @param rnd Random number generator.
	 * @since 1.0.0
	 */
	public static void shuffle(int[] vs, Random rnd) {
		shuffle(0, vs.length, (int a, int b) -> {
			int t = vs[a];
			vs[a] = vs[b];
			vs[b] = t;
		}, rnd);
	}
	
	/**
	 * Shuffles an array of <code>long</code>s.
	 * 
	 * @param vs Array to be shuffled.
	 * @param rnd Random number generator.
	 * @since 1.0.0
	 */
	public static void shuffle(long[] vs, Random rnd) {
		shuffle(0, vs.length, (int a, int b) -> {
			long t = vs[a];
			vs[a] = vs[b];
			vs[b] = t;
		}, rnd);
	}
	
	/**
	 * Shuffles an array of objects.
	 * 
	 * @param <T> Item type.
	 * @param vs Array to be shuffled.
	 * @param rnd Random number generator.
	 * @since 1.0.0
	 */
	public static <T> void shuffle(T[] vs, Random rnd) {
		shuffle(0, vs.length, (int a, int b) -> {
			T t = vs[a];
			vs[a] = vs[b];
			vs[b] = t;
		}, rnd);
	}
	
	/* BINARY CODEC */

	/**
	 * Counts the number of vertices in an undirected graph encoded as a {@link BigInteger}.
	 * 
	 * @param v Graph (integer).
	 * @return Number of vertices.
	 * @since 1.0.0
	 */
	public static int verticesOfUndirected(BigInteger v) {
		return verticesOfUndirected(v.bitLength());
	}

	/**
	 * Counts the number of vertices in an undirected graph encoded as a binary.
	 * 
	 * @param v Length of the binary (in bits).
	 * @return Number of bits.
	 * @since 1.0.0
	 */
	public static int verticesOfUndirected(int bits) {
		if (bits==0) {
			return 0;
		}
		int vn = 1+(int)Math.floor(Math.sqrt(bits*2));

		if (bits > ((vn*vn-vn)>>1)) {
			vn++;
		}
		return vn;
	}

	/**
	 * Decodes an undirected graph encoded as a binary.
	 * 
	 * @param v Graph (binary).
	 * @return Graph as a predicate.
	 * @since 1.0.0
	 */
	public static IntBinaryPredicate decodeUndirected(IntPredicate v) {
		return (int a, int b) -> a!=b && (a>b
				? v.test((a*a-a)/2 + b)
				: v.test((b*b-b)/2 + a));
	}
	/**
	 * Decodes an undirected graph encoded as a {@link BigInteger}.
	 * 
	 * @param v Graph (integer).
	 * @return Graph as a predicate.
	 * @since 1.0.0
	 */
	public static IntBinaryPredicate decodeUndirected(BigInteger v) {
		return decodeUndirected(v::testBit);
	}
	
	/**
	 * Encodes an undirected graph into a {@link BigInteger}.
	 * 
	 * @param ln Number of vertices.
	 * @param fn Graph as predicate.
	 * @return An encoded graph.
	 * @since 1.0.0
	 */
	public static BigInteger encodeUndirected(int ln, IntBinaryPredicate fn) {
		BigInteger[] rt = new BigInteger[] { BigInteger.ZERO };
		encodeUndirected(ln, fn, (int b) -> rt[0] = rt[0].setBit(b));
		return rt[0];
	}

	/**
	 * Encodes an undirected graph.
	 * 
	 * @param ln Number of vertices.
	 * @param fn Graph as predicate.
	 * @param fr Function to be called for every non-zero bit.
	 * @since 1.0.0
	 */
	public static void encodeUndirected(int ln, IntBinaryPredicate fn, IntConsumer fr) {
		int p=0;
		for (int i=1; i<ln; i++) {
			for (int j=0; j<i; j++) {
				if (fn.test(j,i)) {
					fr.accept(p);
				}
				p++;
			}
		}
	}
	
	/* RANDOM */

	/**
	 * Generates a randomized string of bits.
	 * Propability that a bit is set is <code>limit + (1-2*limit)*density</code>.
	 * 
	 * @param ln Number of random bits to return.
	 * @param density Density (0.0 to 1.0, or NaN for random density).
	 * @param limit Density limit.
	 * @param rnd Random number generator.
	 * @return A random string of bits as a predicate.
	 * @since 1.0.0
	 */
	public static IntPredicate randomBinary(int ln, double density, double limit, Random rnd) {
		if (Double.isNaN(density)) {
			density = rnd.nextDouble();
		}
		density = limit + (1-2*limit)*density;
		long[] rt = new long[(ln>>6)+1];
		for (int i=0; i<rt.length; i++) {
			for (int j=0; j<64; j++) {
				if (rnd.nextDouble()<=density) {
					rt[i] |= 1l<<j;
				}
			}
		}
		return (int p) -> (rt[p>>6] & (1l<<(p&63)))!=0;
	}

	/**
	 * Generates a randomized order of symbols.
	 * 
	 * @param ln Number and range of symbols.
	 * @param rnd Random number generator.
	 * @return A random order.
	 * @since 1.0.0
	 */
	public static IntUnaryOperator randomOrder(int ln, Random rnd) {
		int[] ret = new int[ln];
		for (int i=0; i<ln; i++) {
			ret[i] = i;
		}
		shuffle(ret, rnd);
		return (int p) -> ret[p];
	}

	/**
	 * (P) Generates a complex random graph.
	 * 
	 * @param ln Number and range of symbols.
	 * @param density Density (0.0 to 1.0, or NaN for random density).
	 * @param symmetry Symmetry (0.0 to 1.0, or NaN for random symmetry).
	 * @param rnd Random number generator.
	 * @return A random graph as a predicate.
	 * @see #randomUndirectedAsymmetric(int, double, Random)
	 * @see #randomUndirectedSymmetric(int, double, Random)
	 * @see #randomBinary(int, double, double, Random)
	 * @since 1.0.0
	 */
	public static IntBinaryPredicate randomUndirected(int ln, double density, double symmetry, Random rnd) {
		if (ln<3) {
			return ln>0 && rnd.nextBoolean() ? UNDIRECTED_COMPLETE : UNDIRECTED_EMPTY;
		} 

		double cs;
		if (Double.isNaN(symmetry)) {
			cs = rnd.nextDouble();
		} else if (symmetry<=0) {
			return randomUndirectedAsymmetric(ln, density, rnd);
		} else if (symmetry>=1) {
			return randomUndirectedSymmetric(ln, density, rnd);
		} else {
			cs = symmetry;
		}

		if (ln<6) {
			return rnd.nextDouble()<=cs
					? randomUndirectedSymmetric(ln, density, rnd)
					: randomUndirectedAsymmetric(ln, density, rnd);
		}
		
		double va = rnd.nextDouble();
		double vb = rnd.nextDouble();
		if (va<cs && vb<cs) {
			return randomUndirectedSymmetric(ln, density, rnd);
		} else if (va>cs && vb>cs) {
			return randomUndirectedAsymmetric(ln, density, rnd);
		}
		
		int sl = (int)Math.ceil(Math.sqrt(ln));
		int nln = (int)Math.ceil(ln/(double)sl);
		long rd = rnd.nextLong();
		IntBinaryPredicate[] ss = populate(IntBinaryPredicate.class, nln+1, (int p) -> {
			int sln = p==nln ? nln : p==nln-1 && ln%sl!=0 ? ln%sl : sl;
			return randomUndirected(sln, density, symmetry, new Random(rd*(1+p)));
		});
		IntUnaryOperator od = randomOrder(ln, rnd);

		return (int a, int b) -> {
			a = od.applyAsInt(a);
			b = od.applyAsInt(b);
			int ap = a%sl;
			int bp = b%sl;
			int ag = (a-ap)/sl;
			int bg = (b-bp)/sl;
			return ag==bg ? ss[ag].test(ap, bp) : ss[nln].test(ag, bg);
		};
	}
	
	/**
	 * Generates a random asymmetric graph.
	 * 
	 * @param ln Number and range of symbols.
	 * @param density Density (0.0 to 1.0, or NaN for random density).
	 * @param rnd Random number generator.
	 * @return A random graph as predicate.
	 * @see #randomBinary(int, double, double, Random)
	 * @since 1.0.0
	 */
	public static IntBinaryPredicate randomUndirectedAsymmetric(int ln, double density, Random rnd) {
		if (ln<2) {
			return UNDIRECTED_EMPTY;
		}
		ln--;
		
		IntPredicate cq = randomBinary((ln*ln - ln)/2 +1, density, 1.0/ln, rnd);
		return (int a, int b) -> {
			if (a==b) {
				return false;
			} else if (a>b) {
				int t = a;
				a = b;
				b = t;
			}
			b--;
			return cq.test((b*b - b)/2 + a);
		};
	}

	/**
	 * Generates a random symmetric graph.
	 * 
	 * @param ln Number and range of symbols.
	 * @param density Density (0.0 to 1.0, or NaN for random density).
	 * @param rnd Random number generator.
	 * @return A random graph as predicate.
	 * @see #randomBinary(int, double, double, Random)
	 * @since 1.0.0
	 */
	public static IntBinaryPredicate randomUndirectedSymmetric(int ln, double density, Random rnd) {
		if (ln<2) {
			return UNDIRECTED_EMPTY;
		}
		
		IntUnaryOperator od = randomOrder(ln, rnd);
		IntPredicate cq = randomBinary(ln>>1, density, 1.0/ln, rnd);
		return (int a, int b) -> {
			if (a==b) {
				return false;
			}
			int rt = Math.abs(od.applyAsInt(a)-od.applyAsInt(b));
			return cq.test(Math.min(rt, ln-rt)-1);
		};
	}
	
	/* LAMBDA CONVERSIONS */
	
	/**
	 * Wraps a binary operator into binary predicate (undirected graph).
	 * 
	 * @param g Binary operator.
	 * @param fn Value predicate.
	 * @return Binary predicate.
	 * @since 1.0.0
	 */
	public static IntBinaryPredicate asPredicate(IntBinaryOperator g, IntPredicate fn) {
		return (int a, int b) -> fn.test(g.applyAsInt(a, b));
	}
	
	/**
	 * Wraps a binary predicate (undirected graph) into a binary operator.
	 * 
	 * @param g Binary predicate.
	 * @param vv Vertex weight.
	 * @param ve Edge weight.
	 * @param vn Non-edge weight.
	 * @return Binary operator.
	 * @since 1.0.0
	 */
	public static IntBinaryOperator asOperator(IntBinaryPredicate g, int vv, int ve, int vn) {
		return (int a, int b) -> a==b ? vv : g.test(a,b) ? ve : vn;
	}
	
	/**
	 * Wraps a binary predicate (undirected graph) into a binary operator, using 0 for
	 * vertices, 1 for edges and -1 for non-edges.
	 * 
	 * @param g Binary predicate.
	 * @return Binary operator.
	 * @see #asOperator(IntBinaryPredicate, int, int, int)
	 * @since 1.0.0
	 */
	public static IntBinaryOperator asOperator(IntBinaryPredicate g) {
		return asOperator(g, 0, 1, -1);
	}

	/* SEARCH (*) */

	/**
	 * Binary search with input supplied as a lambda which is called for every index
	 * to be evaluated.
	 * 
	 * <p>Example: <code>int rt = search(0, items.length, (int p) -> items[p].compareTo(item));</code>
	 * 
	 * @param from Start of range (inclusive).
	 * @param to End of range (exclusive).
	 * @param fn Lambda to be called for every index to be evaluated; returns 0 if argument is a result; -1 if result is after the argument or 1 if it is before the argument. 
	 * @return Result index; if no result is found, binary negation of the index, where the result should exists, is returned.
	 * @since 1.0.0
	 */
	public static int search(int from, int to, IntUnaryOperator fn) {
		to--;
		while (from <= to) {
			int h = (from + to) >>> 1;
			int cv = fn.applyAsInt(h);
			if (cv < 0) {
				from = h + 1;
			} else if (cv > 0) {
				to = h - 1;
			} else {
				return h;
			}
		}
		return ~from;
	}
	
	/**
	 * Linear search with input supplied as a lambda which is called for every index
	 * to be evaluated; returns first match.
	 * 
	 * <p>If the start of the range is less than the end of the range the search is reversed.</p> 
	 * 
	 * @param from Start of range (inclusive).
	 * @param to End of range (exclusive).
	 * @param fn Predicate; returns true if argument is a result.
	 * @return Result index; if no result is found, the next index after last (to be searched) is returned.
	 * @since 1.0.0
	 */
	public static int search(int from, int to, IntPredicate fn) {
		int step;
		if (from>to) {
			to--;
			from--;
			step = -1;
		} else {
			step = 1;
		}
		
		while (from!=to) {
			if (fn.test(from)) {
				return from;
			}
			from+=step;
		}
		return from;
	}
	
	/**
	 * Linear search with input supplied as a lambda which is called for every index
	 * to be evaluated; returns best match.
	 * 
	 * <p>If the start of the range is less than the end of the range the search is reversed.</p> 
	 * 
	 * @param from Start of range (inclusive).
	 * @param to End of range (exclusive).
	 * @param fn Binary predicate; returns true if first argument is better result than second argument.
	 * @return Result index.
	 * @since 1.0.0
	 */
	public static int search(int from, int to, IntBinaryPredicate fn) {
		int step;
		if (from>to) {
			to--;
			from--;
			step = -1;
		} else {
			step = 1;
		}

		int ret = from;
		from++;
		while (from!=to) {
			if (fn.test(from, ret)) {
				ret = from;
			}
			from+=step;
		}
		return ret;
	}
	
	/* COMPARE */
	
	/**
	 * Compares a pair of arrays of <code>int</code>s, first by length and then by elements if necessary.
	 * 
	 * @param a The first array.
	 * @param b The second array
	 * @param cmp Comparator for element comparision.
	 * @return Negative if the first array is less than the second, positive if it is more or zero if both arrays are equivalent.
	 * @since 1.0.0
	 */
	public static int compare(int[] a, int[] b, IntBinaryOperator cmp) {
		return a.length!=b.length
				? Integer.compare(a.length, b.length)
				: compare(a.length, (int p) -> cmp.applyAsInt(a[p], b[p]));
	}

	/**
	 * Compares a pair of arrays of <code>long</code>s, first by length and then by elements if necessary.
	 * 
	 * @param a The first array.
	 * @param b The second array
	 * @param cmp Comparator for element comparision.
	 * @return Negative if the first array is less than the second, positive if it is more or zero if both arrays are equivalent.
	 * @since 1.0.0
	 */
	public static int compare(long[] a, long[] b, LongToIntBinaryOperator cmp) {
		return a.length!=b.length
				? Integer.compare(a.length, b.length)
				: compare(a.length, (int p) -> cmp.applyAsInt(a[p], b[p]));
	}
	
	/**
	 * Compares a pair of arrays of objects, first by length and then by elements if necessary.
	 * 
	 * @param a The first array.
	 * @param b The second array
	 * @param cmp Comparator for element comparision.
	 * @return Negative if the first array is less than the second, positive if it is more or zero if both arrays are equivalent.
	 * @since 1.0.0
	 */
	public static <T> int compare(T[] a, T[] b, Comparator<T> cmp) {
		return a.length!=b.length
				? Integer.compare(a.length, b.length)
				: compare(a.length, (int p) -> cmp.compare(a[p], b[p]));
	}
	
	/**
	 * Combines several comparators. In practice this methods executes the supplied lambdas one by one
	 * and returns the first non-zero value, or zero if all lambdas return zero.
	 * 
	 * @param fns Array of comparators.
	 * @return First non-zero value returned by a comparator.
	 * @see #compare(int, IntUnaryOperator)
	 * @since 1.0.0
	 */
	public static int compare(IntSupplier... fns) {
		return compare(fns.length, (int p) -> fns[p].getAsInt());
	}

	/**
	 * Combines several comparators. In practice this methods executes the supplied lambdas one by one
	 * and returns the first non-zero value, or zero if all lambdas return zero.
	 * 
	 * @param ln Number of comparators.
	 * @param fn Applies nth comparator.
	 * @return First non-zero value returned by a comparator.
	 * @see #compare(IntSupplier...)
	 * @since 1.0.0
	 */
	public static int compare(int ln, IntUnaryOperator fn) {
		int ret = 0;
		for (int i=0; ret==0 && i<ln; i++) {
			ret = fn.applyAsInt(i);
		}
		return ret;
	}
	
}
