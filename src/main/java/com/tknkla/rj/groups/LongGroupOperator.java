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

import com.tknkla.rj.RJ;
import com.tknkla.rj.functions.LongToIntBinaryOperator;

/**
 * Group operator interface for <code>long</code>s.
 * 
 * @author Timo Santasalo
 * 
 * @see RJ#propagate(long[][], java.util.function.LongBinaryOperator, LongToIntBinaryOperator, LongGroupOperator, java.util.function.Predicate)
 * @since 1.0.0
 */
public interface LongGroupOperator {

	/**
	 * The default group operator (additive).
	 * @since 1.0.0
	 */
	public static final LongGroupOperator ADDITIVE = new LongGroupOperator() {

		@Override
		public int signum(long v) {
			return Long.signum(v);
		}
		
		@Override
		public long identityAsLong() {
			return 0;
		}

		@Override
		public long applyAsLong(long a, long b) {
			return a+b;
		}

		@Override
		public long cancelAsLong(long a, long b) {
			return a-b;
		}

	};

	/**
	 * Returns the identity element.
	 * @return The identity element.
	 * @since 1.0.0
	 */
	long identityAsLong();

	/**
	 * Applies the (commutative) group operation. 
	 * @param a Left element.
	 * @param b Right element.
	 * @return Result of the group operation.
	 * @since 1.0.0
	 */
	long applyAsLong(long a, long b);
	
	/**
	 * Applies the (non-commutative) inverse group operation.
	 * @param a Left element.
	 * @param b Right element.
	 * @return Result of the group operation.
	 * @since 1.0.0
	 */
	long cancelAsLong(long a, long b);
	
	/**
	 * Returns sign of an element; inversion of an element negates it's sign.
	 * @param v Group element.
	 * @return Sign of the element.
	 * @since 1.0.0
	 */
	int signum(long v);
	
}
