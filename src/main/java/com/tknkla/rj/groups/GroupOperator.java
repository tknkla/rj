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

import java.math.BigInteger;
import java.util.function.IntFunction;

import com.tknkla.rj.RJ;

/**
 * Generic group operator interface for arbitrary objects.
 * 
 * @author Timo Santasalo
 * 
 * @see RJ#propagate(Class, Object[][], java.util.function.BiFunction, java.util.Comparator, GroupOperator, java.util.function.Predicate)
 *
 * @param <T> Element type.
 * @since 1.0.0
 */
public interface GroupOperator<T> {
	
	/**
	 * The default group operator (additive).
	 * @since 1.0.0
	 */
	public static final GroupOperator<BigInteger> BIGINTEGER_ADDITIVE = new GroupOperator<BigInteger>() {

		@Override
		public int signum(BigInteger v) {
			return v.signum();
		}
		
		@Override
		public BigInteger identity() {
			return BigInteger.ZERO;
		}
		
		@Override
		public BigInteger apply(BigInteger a, BigInteger b) {
			return a.add(b);
		}
		
		@Override
		public BigInteger cancel(BigInteger a, BigInteger b) {
			return a.subtract(b);
		}

	};

	/**
	 * Returns the identity element.
	 * @return The identity element.
	 * @since 1.0.0
	 */
	T identity();

	/**
	 * Applies the (commutative) group operation (binary).
	 * @param a Left element.
	 * @param b Right element.
	 * @return Result of the group operation.
	 * @since 1.0.0
	 */
	T apply(T a, T b);	

	/**
	 * Applies the (non-commutative) inverse group operation.
	 * @param a Left element.
	 * @param b Right element.
	 * @return Result of the group operation.
	 * @since 1.0.0
	 */
	T cancel(T a, T b);
	
	/**
	 * Returns sign of an element; inversion of an element negates it's sign.
	 * @param v Group element.
	 * @return Sign of the element.
	 * @since 1.0.0
	 */
	int signum(T v);
	
	/**
	 * Applies the (commutative) group operator (anary).
	 * @param from Index of the first element (inclusive).
	 * @param to Index of the last element (exclusive).
	 * @param fn Function to supply the nth element.
	 * @return Result of the group operation.
	 * @since 1.1.0
	 */
	default T apply(int from, int to, IntFunction<T> fn) {
		return RJ.execute(from, to, identity(), fn, this::apply);
	}
	
}
