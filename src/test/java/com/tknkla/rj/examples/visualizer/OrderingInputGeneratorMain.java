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

import java.util.Arrays;
import java.util.Random;
import java.util.function.IntBinaryOperator;

import com.tknkla.rj.RJ;
import com.tknkla.rj.functions.IntBinaryPredicate;

/**
 * Generates inputs for {@link OrderingVisualizerMain}.
 * 
 * <p>Usage: <code>java ...OrderingInputGeneratorMain [vertices] [density] [symmetry]</code>
 * 
 * @see RJ#randomUndirected(int, double, double, Random)
 * @author Timo Santasalo
 */
public class OrderingInputGeneratorMain extends AbstractOrderAlgorithm implements Comparable<OrderingInputGeneratorMain> {
	
	public int pivots = 0;
	public int orders = 0;
	public int propagates = 0;
	
	private int[][] last = null;

	public OrderingInputGeneratorMain(int ln, IntBinaryOperator fg) {
		super(ln, fg);
		order();
	}
	
	@Override
	public String toString() {
		return orders+"\t"+propagates+"\t"+pivots+"\t"+toBigInteger();
	}

	@Override
	protected void beforePivot(int[][] order, int pv) {
		pivots++;
	}

	@Override
	protected void afterPivot(int[][] order) {}

	@Override
	protected void beforeOrdering(int[][] order) {
		last = order;
	}

	@Override
	protected void afterOrdering(int[][] order) {
		if (!Arrays.deepEquals(order, last)) {
			orders++;
		}
	}

	@Override
	protected void beforePropagate(int[][] order) {}

	@Override
	protected void iterPropagate(int[][] order) {
		propagates++;
	}

	@Override
	protected void afterPropagate(int[][] order) {}

	@Override
	public int compareTo(OrderingInputGeneratorMain o) {
		if (orders==o.orders) {
			if (propagates==o.propagates) {
				return o.pivots-pivots;
			} else {
				return propagates-o.propagates;
			}
		} else {
			return orders-o.orders;
		}
	}

	public static void run(Random rnd, int ln, double density, double symmetry) {
		OrderingInputGeneratorMain cr = null;
		while (true) {
			IntBinaryPredicate g = RJ.randomUndirected(ln, density, symmetry, rnd);
			int[][] rs = RJ.populate(ln, ln, RJ.asOperator(g));
			OrderingInputGeneratorMain rt = new OrderingInputGeneratorMain(ln, (int a, int b) -> rs[a][b]);
			if (cr==null || cr.compareTo(rt)<0) {
				cr = rt;
				System.out.println(ln+":\t"+cr);

			}
		}
	}
		
	public static void main(String[] args) {
		run(new Random(), Integer.parseInt(args[0]), Double.parseDouble(args[1]), Double.parseDouble(args[2]));
	}
	
}
