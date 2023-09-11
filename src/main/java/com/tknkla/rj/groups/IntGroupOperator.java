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
package com.tknkla.rj.groups;

import java.util.function.IntUnaryOperator;

import com.tknkla.rj.RJ;

/**
 * Group operator interface for <code>int</code>s.
 * 
 * @author Timo Santasalo
 * 
 * @see RJ#propagate(int[][], java.util.function.IntBinaryOperator, java.util.function.IntBinaryOperator, IntGroupOperator, java.util.function.Predicate)
 * @since 1.0.0
 */
public interface IntGroupOperator {

	/**
	 * The default group operator (additive).
	 * @since 1.0.0
	 */
	public static final IntGroupOperator ADDITIVE = new IntGroupOperator() {
		
		@Override
		public int signum(int v) {
			return v;
		}
		
		@Override
		public int identityAsInt() {
			return 0;
		}
		
		@Override
		public int applyAsInt(int a, int b) {
			return a+b;
		}

		@Override
		public int cancelAsInt(int a, int b) {
			return a-b;
		}

	};

	/**
	 * Returns the identity element.
	 * @return The identity element.
	 * @since 1.0.0
	 */
	int identityAsInt();
	
	/**
	 * Applies the (commutative) group operation (binary). 
	 * @param a Left element.
	 * @param b Right element.
	 * @return Result of the group operation.
	 * @since 1.0.0
	 */
	int applyAsInt(int a, int b);
	
	/**
	 * Applies the (non-commutative) inverse group operation.
	 * @param a Left element.
	 * @param b Right element.
	 * @return Result of the group operation.
	 * @since 1.0.0
	 */
	int cancelAsInt(int a, int b);
	
	/**
	 * Returns sign of an element; inversion of an element negates it's sign.
	 * @param v Group element.
	 * @return Sign of the element.
	 * @since 1.0.0
	 */
	int signum(int v);
	
	/**
	 * Applies the (commutative) group operator (anary).
	 * @param from Index of the first element (inclusive).
	 * @param to Index of the last element (exclusive).
	 * @param fn Function to supply the nth element.
	 * @return Result of the group operation.
	 * @since 1.1.0
	 */
	default int applyAsInt(int from, int to, IntUnaryOperator fn) {
		return RJ.execute(from, to, identityAsInt(), fn, this::applyAsInt);
	}

}
