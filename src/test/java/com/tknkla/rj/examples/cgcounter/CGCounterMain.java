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
package com.tknkla.rj.examples.cgcounter;

import java.math.BigInteger;

import com.tknkla.rj.RJ;
import com.tknkla.rj.functions.IntBinaryPredicate;
import com.tknkla.rj.groups.IntGroupOperator;

/**
 * Counts canonical graphs.
 * 
 * <p>Outputs:
 * <pre>
 * V	N	M
 * 0	0	0
 * 1	1	1
 * 2	1	1
 * 3	3	6
 * 4	10	56
 * 5	33	960
 * 6	155	31744
 * 7	1043	2064384
 * 8	12345	266338304
 * ...
 * </pre>
 * @author Timo Santasalo
 */
public class CGCounterMain {
	
	private static boolean isCanon(int ln, IntBinaryPredicate g) {
		if (ln<3) {
			return true;
		}
		int[] rt = RJ.order(new int[][] { RJ.populate(ln, (int p) -> p) },
				RJ.asOperator(g),
				IntGroupOperator.ADDITIVE,
				Integer::compare);
		for (int i=0; i<ln; i++) {
			if (rt[i]!=i) {
				return false;
			}
		}
		return true;
	}

	public static void main(String[] args) {
		long cn = 0;
		long gs=0, cc=0, tn=0;
		System.out.println("V\tN\tM");
		while (true) {
			BigInteger v = BigInteger.valueOf(cn);
			int ln = RJ.verticesOfUndirected(v);
			if (ln>gs) {
				System.out.println(gs+"\t"+cc+"\t"+tn);
				gs = ln;
				cc=0;
				tn=0;
			}
			tn++;
			
			if (isCanon(ln, RJ.decodeUndirected(v))) {
				cc++;
			}
			cn++;
		}

	}
	
	/*
	 */

}
