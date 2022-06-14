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
package com.tknkla.rj.examples.visualizer;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.function.IntBinaryOperator;
import java.util.function.Predicate;

import com.tknkla.rj.RJ;
import com.tknkla.rj.groups.IntGroupOperator;

/**
 * Partial re-implementation of {@link RJ#order(int[][], IntBinaryOperator, IntGroupOperator, IntBinaryOperator)} with callbacks.
 * 
 * @author Timo Santasalo
 */
public abstract class AbstractOrderAlgorithm {
	
	public final int ln;
	public final IntBinaryOperator fg;
	
	public int[] od;
	public int[] odi;
	
	public int[][] gs;
	public int[] gsi;
		
	public AbstractOrderAlgorithm(int ln, IntBinaryOperator fg) {
		this.ln = ln;
		this.fg = fg;
	}
	
	protected void order() {
		od = order(new int[][] { RJ.populate(ln, (int p) -> p) }, IntGroupOperator.ADDITIVE, Integer::compare);
		odi = new int[od.length];
		gsi = new int[od.length];
		for (int i=0; i<od.length; i++) {
			odi[od[i]] = i;
		}
		for (int i=0; i<gs.length; i++) {
			for (int j=0; j<gs[i].length; j++) {
				gsi[gs[i][j]] = i;
			}
		}
	}
	
	public BigInteger toBigInteger() {
		return RJ.encodeUndirected(ln, (int a, int b) -> fg.applyAsInt(a,b)>0);
	}
	
	protected abstract void beforePivot(int[][] order, int pv);
	protected abstract void afterPivot(int[][] order);
	protected abstract void beforeOrdering(int[][] order);
	protected abstract void afterOrdering(int[][] order);
	protected abstract void beforePropagate(int[][] order);
	protected abstract void iterPropagate(int[][] order);
	protected abstract void afterPropagate(int[][] order);

	private int[] order(int[][] src, IntGroupOperator g, IntBinaryOperator cmp) {
		while (true) {
			src = groups(src, g);
			int pv = -1;
			for (int i=src.length-1; i>=0; i--) {
				if (src[i].length>1) {
					pv = i;
					break;
				}
			}
			if (pv==-1) {
				break;
			}
			beforePivot(src, pv);
			int[][] po = new int[][] { Arrays.copyOfRange(src[pv], 1, src[pv].length), { src[pv][0] } };
			if (cmp!=null) {
				for (int i=0; i<po[0].length; i++) {
					if (po[1][0] < po[0][i]) {
						int t = po[0][i];
						po[0][i] = po[1][0];
						po[1][0] = t;
					}
				}
			}
			po = propagate(po, g, null);
			int[][] nsrc = new int[src.length+po.length-1][];
			System.arraycopy(src, 0, nsrc, 0, pv);
			System.arraycopy(po, 0, nsrc, pv, po.length);
			System.arraycopy(src, pv+1, nsrc, pv+po.length, src.length-pv-1);
			afterPivot(nsrc);
			src = nsrc;
		}
		int[][] _src = src;
		return RJ.populate(src.length, (int p) -> _src[p][0]);
	}
	
	private int[][] groups(int[][] src, IntGroupOperator g) {
		int[][] _src = propagate(src, g, null);
		beforeOrdering(_src);
		int[][] ret = RJ.groups(_src, (int a, int b) -> RJ.compare(a,b, _src, fg, g), null);
		afterOrdering(ret);
		if (gs==null) {
			gs = ret;
		}
		return ret;
	}

	private int[][] propagate(int[][] src, IntGroupOperator g, Predicate<int[][]> fh) {
		beforePropagate(src);
		while (fh==null || fh.test(src)) {
			int[][] _src = src;
			int[][] nsrc = RJ.groups(src, (int a, int b) -> {
				for (int i=_src.length-1; i>=0; i--) {
					int _i = i;
					int rt = g.signum(RJ.execute(0, _src[i].length, g.identityAsInt(), (int p) -> {
						int c = _src[_i][p];
						return g.cancelAsInt(fg.applyAsInt(a, c), fg.applyAsInt(b, c));
					}, g::applyAsInt));
					if (rt!=0) {
						return rt;
					}
				}
				return 0;
			}, null);
			if (nsrc.length==src.length) {
				break;
			}
			iterPropagate(src);
			src = nsrc;
		}
		afterPropagate(src);
		return src;
	}
	
}
