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

/**
 * Truth table of elementary set operations.
 * 
 * @author Timo Santasalo
 * 
 * @see RJ#merge(int[], int[], java.util.function.IntBinaryOperator, java.util.function.IntBinaryOperator, SetOperator)
 * @see RJ#merge(long[], long[], LongToIntBinaryOperator, java.util.function.LongBinaryOperator, SetOperator)
 * @see RJ#merge(Object[], Object[], java.util.Comparator, java.util.function.BinaryOperator, SetOperator)
 * @since 1.0.0
 */
public enum SetOperator {

	/**
	 * Always returns an empty set.
	 */
	EMPTY(false, false, false),
	
	/**
	 * Intersection.
	 */
	ISECT(false, false, true),
	
	/**
	 * Right asymmetric difference.
	 */
	RDIFF(false, true, false),
	
	/**
	 * Always returns right argument.
	 */
	RIGHT(false, true, true),
	
	/**
	 * Left asymmetric difference.
	 */
	LDIFF(true, false, false),
	
	/**
	 * Always returns left argument.
	 */
	LEFT(true, false, true),

	/**
	 * Symmetric difference.
	 */
	DIFF(true, true, false),

	/**
	 * Union.
	 */
	UNION(true, true, true);
	
	/**
	 * If true, accepts element that is in left but not in right argument.
	 * @since 1.0.0
	 */
	public final boolean left;
	/**
	 * If true, accepts element that is in right but not in left argument.
	 * @since 1.0.0
	 */
	public final boolean right;
	/**
	 * If true, accepts element that is in both arguments.
	 * @since 1.0.0
	 */
	public final boolean both;
	
	private SetOperator(boolean left, boolean right, boolean both) {
		this.left = left;
		this.right = right;
		this.both = both;
	}
	
	public SetOperator reverse() {
		return of(right, left, both);
	}
	
	/**
	 * Returns a set operator based on provided truth table.
	 * 
	 * @param left If true, accepts element that is in left but not in right argument.
	 * @param right If true, accepts element that is in right but not in left argument.
	 * @param both If true, accepts element that is in both arguments.
	 * @return A set operator.
	 * @since 1.0.0
	 */
	public static SetOperator of(boolean left, boolean right, boolean both) {
		return values()[(left ? 4 : 0)+(right ? 2 : 0)+(both ? 1 : 0)];
	}
	

	/**
	 * Utility method used for internal input validation.
	 * 
	 * @throws IllegalArgumentException If this is an asymmetric operator (if {@link #left} != {@link #right}).
	 */
	void requireSymmetric() {
		if (left!=right) {
			throw new IllegalArgumentException(this+" is not symmetric");
		}
	}
}
