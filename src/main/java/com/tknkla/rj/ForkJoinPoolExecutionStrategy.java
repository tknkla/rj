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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ManagedBlocker;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Parallel execution strategy based on {@link ForkJoinPool}.
 * 
 * @author Timo Santasalo
 */
public class ForkJoinPoolExecutionStrategy implements ExecutionStrategy {
	
	private final ForkJoinPool xs;
	private final int workSizeFactor;
	private final int concurrency;
	
	private final ThreadLocal<Integer> cd = ThreadLocal.withInitial(() -> 0);

	/**
	 * Default constructor.
	 * 
 	 * <p>Forks if <code>depth &lt; concurrency</code> and <code>size &gt; workSizeFactor*(depth^2+1)</code></p>.
 	 * 
	 * @param xs Thread pool.
	 * @param forkCond Forking condition (first argument is task size, second argument is fork depth).
	 * @param workSizeFactor Minimal work size.
	 * @param concurrency Concurrency number (base-2 logarithm of number of threads).
	 */
	public ForkJoinPoolExecutionStrategy(ForkJoinPool xs, int workSizeFactor, int concurrency) {
		super();
		this.xs = xs;
		this.workSizeFactor = workSizeFactor;
		this.concurrency = concurrency;
	}
	
	/**
	 * Returns the threadpool.
	 * @return The threadpool.
	 */
	public ForkJoinPool getPool() {
		return xs;
	}
	
	@Override
	public void execute(Consumer<Runnable> fn) {
		CountDownLatch cdl = new CountDownLatch(1);
		fn.accept(cdl::countDown);
		try {
			ForkJoinPool.managedBlock(new ManagedBlocker() {
				@Override
				public boolean isReleasable() {
					return cdl.getCount()==0;
				}
				
				@Override
				public boolean block() throws InterruptedException {
					cdl.await();
					return true;
				}
			});
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void queue(Consumer<Runnable> a, Consumer<Runnable> b, Runnable c) {
		AtomicBoolean rs = new AtomicBoolean();
		Runnable nc = () -> {
				if (!rs.compareAndSet(false, true)) {
					c.run();
				}
			};

		int rd = cd.get();
		try {
			xs.execute(() -> {
				cd.set(rd+1);
				a.accept(nc);
			});
			cd.set(rd+1);
			b.accept(nc);
		} finally {
			cd.set(rd);
		}
	}

	@Override
	public boolean fork(int taskSize) {
		int depth = cd.get();
		return taskSize>workSizeFactor*(depth*depth+1) && depth<concurrency;
	}
	
}
