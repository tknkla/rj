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

import org.junit.Assert;

public abstract class TestSupport extends Assert {

	public static int[] toInt(long[] vs) {
		int[] ret = new int[vs.length];
		for (int i=0; i<vs.length; i++) {
			ret[i] = (int) vs[i];
		}
		return ret;
	}
	
	public static int[] toInt(BigInteger[] vs) {
		int[] ret = new int[vs.length];
		for (int i=0; i<vs.length; i++) {
			ret[i] = vs[i].intValueExact();
		}
		return ret;
	}
	
	public static long[] toLong(int[] vs) {
		long[] ret = new long[vs.length];
		for (int i=0; i<vs.length; i++) {
			ret[i] = vs[i];
		}
		return ret;
	}
	
	public static BigInteger[] toBigInteger(int[] vs) {
		BigInteger[] ret = new BigInteger[vs.length];
		for (int i=0; i<vs.length; i++) {
			ret[i] = BigInteger.valueOf(vs[i]);
		}
		return ret;
	}
	

	public static int[][] toInt(long[][] vs) {
		int[][] ret = new int[vs.length][];
		for (int i=0; i<vs.length; i++) {
			ret[i] = toInt(vs[i]);
		}
		return ret;
	}
	
	public static int[][] toInt(BigInteger[][] vs) {
		int[][] ret = new int[vs.length][];
		for (int i=0; i<vs.length; i++) {
			ret[i] = toInt(vs[i]);
		}
		return ret;
	}
	
	public static long[][] toLong(int[][] vs) {
		long[][] ret = new long[vs.length][];
		for (int i=0; i<vs.length; i++) {
			ret[i] = toLong(vs[i]);
		}
		return ret;
	}
	
	public static BigInteger[][] toBigInteger(int[][] vs) {
		BigInteger[][] ret = new BigInteger[vs.length][];
		for (int i=0; i<vs.length; i++) {
			ret[i] = toBigInteger(vs[i]);
		}
		return ret;
	}
	
	public static void assertEquals(int[] expected, int[] iactual, long[] lactual, BigInteger[] oactual) {
		System.out.println(Arrays.toString(expected)+" ?=\n\t"+
				Arrays.toString(iactual)+"\n\t"+
				Arrays.toString(lactual)+"\n\t"+
				Arrays.toString(oactual)+"\n\t");
		assertArrayEquals(expected, iactual);
		assertArrayEquals(expected, toInt(lactual));
		assertArrayEquals(expected, toInt(oactual));
	}

	public static void assertEquals(int[][] expected, int[][] iactual, long[][] lactual, BigInteger[][] oactual) {
		System.out.println(Arrays.deepToString(expected)+" ?=\n\t"+
				Arrays.deepToString(iactual)+"\n\t"+
				Arrays.deepToString(lactual)+"\n\t"+
				Arrays.deepToString(oactual)+"\n\t");
		assertArrayEquals(expected, iactual);
		assertArrayEquals(expected, toInt(lactual));
		assertArrayEquals(expected, toInt(oactual));
	}

}
