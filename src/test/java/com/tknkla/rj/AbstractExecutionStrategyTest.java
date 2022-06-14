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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractExecutionStrategyTest extends Assert {

	public static final long TIMEOUT = 100;
	
	private final ExecutionStrategy xs;

	public AbstractExecutionStrategyTest(ExecutionStrategy xs) {
		super();
		this.xs = xs;
	}

	public void test(int depth, Runnable fn, Runnable fh) {
		if (depth==0) {
			try {
				fn.run();
			} finally {
				fh.run();
			}
		} else {
			//assertTrue(xs.fork(1<<depth));
			Consumer<Runnable> nr = (Runnable r) -> {
				test(depth-1, fn, r);
			};
			xs.queue(nr, nr, fh);
		}
	}
	
	public void test(int depth, Runnable fn) {
		//assertTrue(xs.fork(1<<depth));
		xs.execute((Runnable r) -> {
			test(depth, fn, r);
		});
	}
	
	public void testExecute(int depth) {
		AtomicInteger rt = new AtomicInteger();
		test(depth, () -> {
			rt.incrementAndGet();
		});		assertEquals(1<<depth, rt.get());
	}

	public void testExecute(int depth1, int depth2) {
		AtomicInteger rt = new AtomicInteger();
		test(depth1, () -> {
			test(depth2, () -> {
				rt.incrementAndGet();
			});
		});
		assertEquals(1<<(depth1+depth2), rt.get());
	}

	@Test(timeout = TIMEOUT)
	public void testExecute0() {
		testExecute(0);
	}

	@Test(timeout = TIMEOUT)
	public void testExecute1() {
		testExecute(1);
	}

	@Test(timeout = TIMEOUT)
	public void testExecute2() {
		testExecute(2);
	}

	@Test(timeout = TIMEOUT)
	public void testExecute4() {
		testExecute(4);
	}

	@Test(timeout = TIMEOUT)
	public void testExecute0_0() {
		testExecute(0, 0);
	}

	@Test(timeout = TIMEOUT)
	public void testExecute0_1() {
		testExecute(0, 1);
	}

	@Test(timeout = TIMEOUT)
	public void testExecute0_2() {
		testExecute(0, 2);
	}

	@Test(timeout = TIMEOUT)
	public void testExecute0_4() {
		testExecute(0, 4);
	}

	@Test(timeout = TIMEOUT)
	public void testExecute2_0() {
		testExecute(2, 0);
	}

	@Test(timeout = TIMEOUT)
	public void testExecute2_1() {
		testExecute(2, 1);
	}

	@Test(timeout = TIMEOUT)
	public void testExecute2_2() {
		testExecute(2, 2);
	}

	@Test(timeout = TIMEOUT)
	public void testExecute2_4() {
		testExecute(2, 4);
	}
	
	@Test(timeout = TIMEOUT)
	public void testExecute4_4() {
		testExecute(4, 4);
	}

	@Test(timeout = TIMEOUT*10)
	public void testExecute8_8() {
		testExecute(8, 8);
	}


}
