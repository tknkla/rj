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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class SimulatedParallel2RJTest extends AbstractSimulatedRJTest {
	
	private static final int STACK_SIZE_MAX = 1024;
	
	private final List<Runnable> stack = new ArrayList<>();

	@Override
	public void execute(Consumer<Runnable> fn) {
		boolean[] rs = new boolean[1];
		stack.add(() -> fn.accept(() -> rs[0] = true));
		assertTrue(stack.size()<=STACK_SIZE_MAX);
		while (!rs[0]) {
			stack.remove(rnd.nextInt(stack.size())).run();
		}
	}

	@Override
	public void queue(Consumer<Runnable> a, Consumer<Runnable> b, Runnable h) {
		boolean[] rs = new boolean[1];
		Runnable nc = () -> {
				if (rs[0]) {
					h.run();
				} else {
					rs[0] = true;
				}
			};
		
		stack.add(() -> a.accept(nc));
		stack.add(() -> b.accept(nc));
		assertTrue(stack.size()<=STACK_SIZE_MAX);
	}

	@Before
	public void before() {
		super.before();
		stack.clear();
	}
	
	@After
	public void after() {
		super.after();
		assertTrue(stack.isEmpty());
	}
	
	@Test
	@Ignore
	@Override
	public void testExecution2() {
		super.testExecution2();
	}
	
	@Test
	@Ignore
	@Override
	public void testOrder2Randoms() {
		super.testOrder2Randoms();
	}

}
