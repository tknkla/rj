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

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.Supplier;

import com.tknkla.rj.functions.TriFunction;

/**
 * Parallel execution strategy.
 * 
 * <p>Parallelization in RJ is based on parallel execution of divide-and-conquer patterns.</p>
 * 
 * <p>This interface defines an execution pattern which attempts to minimize synchronization
 * between threads. Do note that the current interface is not final (semantic versioning applies
 * only partially) and thus custom implementations may not be directly compatible with future
 * versions.</p>
 * 
 * <p>For an example of usage, see source code of {@link RJ#execute(int, int, int, java.util.function.IntUnaryOperator, IntBinaryOperator)}.</p>
 * 
 * @see RJ#setExecutor(ExecutionStrategy)
 * 
 * @author Timo Santasalo
 * @since 1.0.0
 */
public interface ExecutionStrategy {
	
	/**
	 * Default non-parallel execution strategy.
	 * 
	 * <p>{@link #fork(int)} always returns false and every other method throws an {@link IllegalStateException}.
	 * @since 1.0.0
	 */
	static final ExecutionStrategy LOCAL = new ExecutionStrategy() {

		@Override
		public void execute(Consumer<Runnable> fn) {
			throw new IllegalStateException();
		}

		@Override
		public int executeAsInt(Consumer<IntConsumer> fn) {
			throw new IllegalStateException();
		}

		@Override
		public long executeAsLong(Consumer<LongConsumer> fn) {
			throw new IllegalStateException();
		}

		@Override
		public <T> T executeAsObj(Consumer<Consumer<T>> fn) {
			throw new IllegalStateException();
		}

		@Override
		public void queue(Consumer<Runnable> a, Consumer<Runnable> b, Runnable c) {
			throw new IllegalStateException();
		}
		
		@Override
		public void queue(Consumer<IntConsumer> a, Consumer<IntConsumer> b, IntConsumer h, IntBinaryOperator fn) {
			throw new IllegalStateException();
		}

		@Override
		public void queue(Consumer<LongConsumer> a, Consumer<LongConsumer> b, LongConsumer h, LongBinaryOperator fn) {
			throw new IllegalStateException();
		}

		@Override
		public <T, R> void queue(Consumer<Consumer<T>> a, Consumer<Consumer<T>> b, Consumer<R> h,
				BiFunction<T, T, R> fn) {
			throw new IllegalStateException();
		}

		@Override
		public <T, R> void queue(Consumer<Consumer<T>> a, Consumer<Consumer<T>> b, Consumer<Consumer<T>> c,
				Consumer<R> h, TriFunction<T, T, T, R> fn) {
			throw new IllegalStateException();
		}

		@Override
		public boolean fork(int taskSize) {
			return false;
		}
	
	};
	
	/**
	 * Default parallel execution strategy based on {@link ForkJoinPool#commonPool()} (work size is 4).
	 * @since 1.0.0
	 */
	static ExecutionStrategy PARALLEL = ((Supplier<ExecutionStrategy>)(() -> {
		ForkJoinPool cp = ForkJoinPool.commonPool();
		return new ForkJoinPoolExecutionStrategy(cp,
			ForkJoinPoolExecutionStrategy.defaultCondition(32 - Integer.numberOfLeadingZeros(cp.getParallelism()), 4));
		})).get();
	
	/* EXECUTE/LATCH */

	/**
	 * Executes a task, blocking until the execution is complete.
	 * 
	 * <p>{@link #fork(int)} must be queried before invoking this method.</p>
	 * <p>The task must execute the provided {@link Consumer} after it is complete.</p>
	 * <p>If this method is called from inside the task, the implementation must ensure that blocking doesn't deadlock.</p>
	 * <p>The default implementation delegates to {@link #execute(Consumer)}.</p>
	 * 
	 * @param fn Task.
	 */
	void execute(Consumer<Runnable> fn);
	
	/**
	 * Executes a task which returns an <code>int</code>, blocking until the execution is complete.
	 * 
	 * <p>{@link #fork(int)} must be queried before invoking this method.</p>
	 * <p>The task must execute the provided {@link Consumer} after it is complete.</p>
	 * <p>If this method is called from inside the task, the implementation must ensure that blocking doesn't deadlock.</p>
	 * <p>The default implementation delegates to {@link #execute(Consumer)}.</p>
	 * 
	 * @param fn Task.
	 * @return Result.
	 */

	default int executeAsInt(Consumer<IntConsumer> fn) {
		AtomicInteger rt = new AtomicInteger();
		execute((Runnable r) -> fn.accept((int rv) -> {
			try {
				rt.set(rv);
			} finally {
				r.run();
			}
		}));
		return rt.get();
	}
	
	
	/**
	 * Executes a task which returns a <code>long</code>, blocking until the execution is complete.
	 * 
	 * <p>{@link #fork(int)} must be queried before invoking this method.</p>
	 * <p>The task must execute the provided {@link Consumer} after it is complete.</p>
	 * <p>If this method is called from inside the task, the implementation must ensure that blocking doesn't deadlock.</p>
	 * <p>The default implementation delegates to {@link #execute(Consumer)}.</p>
	 * 
	 * @param fn Task.
	 * @return Result.
	 */
	default long executeAsLong(Consumer<LongConsumer> fn) {
		AtomicLong rt = new AtomicLong();
		execute((Runnable r) -> fn.accept((long rv) -> {
			try {
				rt.set(rv);
			} finally {
				r.run();
			}
		}));
		return rt.get();
	}

	
	/**
	 * Executes a task which returns an object, blocking until the execution is complete.
	 * 
	 * <p>{@link #fork(int)} must be queried before invoking this method.</p>
	 * <p>The task must execute the provided {@link Consumer} after it is complete.</p>
	 * <p>If this method is called from inside the task, the implementation must ensure that blocking doesn't deadlock.</p>
	 * <p>The default implementation delegates to {@link #execute(Consumer)}.</p>
	 * 
	 * @param fn Task.
	 * @return Result.
	 */
	default <T> T executeAsObj(Consumer<Consumer<T>> fn) {
		AtomicReference<T> rt = new AtomicReference<>();
		execute((Runnable r) -> fn.accept((T rv) -> {
			try {
				rt.set(rv);
			} finally {
				r.run();
			}
		}));
		return rt.get();
	}

	/* QUEUE */
	
	/**
	 * Queues a pair of tasks for execution.
	 * 
	 * <p>{@link #fork(int)} should be queried before invoking this method.</p>
	 * <p>Both tasks must execute the provided {@link Runnable}s after completion.</p>
	 * 
	 * @param a First task.
	 * @param b Second task.
	 * @param h Runnable to be executed afterward both tasks are finished.
	 */
	void queue(Consumer<Runnable> a, Consumer<Runnable> b, Runnable h);
	
	/**
	 * Queues a pair of tasks for execution.
	 * 
	 * <p>{@link #fork(int)} should be queried before invoking this method.</p>
	 * <p>All task must execute the provided {@link Consumer}s after completion.</p>
	 * <p>The default implementation delegates to {@link #queue(Consumer, Consumer, Runnable)}.</p>
	 * 
	 * @param a First task.
	 * @param b Second task.
	 * @param h Consumer to be executed afterward both tasks are finished.
	 * @param fn Merge operator.
	 */
	default void queue(Consumer<IntConsumer> a, Consumer<IntConsumer> b, IntConsumer h, IntBinaryOperator fn) {
		AtomicInteger ra = new AtomicInteger();
		AtomicInteger rb = new AtomicInteger();
		queue((Runnable r) -> a.accept((int rt) -> {
					try {
						ra.set(rt);
					} finally {
						r.run();
					}
				}),
				(Runnable r) -> b.accept((int rt) -> {
					try {
						rb.set(rt);
					} finally {
						r.run();
					}
				}), () -> { h.accept(fn.applyAsInt(ra.get(), rb.get())); });
	}
	
	/**
	 * Queues a pair of tasks for execution.
	 * 
	 * <p>{@link #fork(int)} should be queried before invoking this method.</p>
	 * <p>Both tasks must execute the provided {@link Consumer}s after completion.</p>
	 * <p>The default implementation delegates to {@link #queue(Consumer, Consumer, Runnable)}.</p>
	 * 
	 * @param a First task.
	 * @param b Second task.
	 * @param h Consumer to be executed afterward both tasks are finished.
	 * @param fn Merge operator.
	 */
	default void queue(Consumer<LongConsumer> a, Consumer<LongConsumer> b, LongConsumer h, LongBinaryOperator fn) {
		AtomicLong ra = new AtomicLong();
		AtomicLong rb = new AtomicLong();
		queue((Runnable r) -> a.accept((long rt) -> {
			try {
				ra.set(rt);
			} finally {
				r.run();
			}
		}),
		(Runnable r) -> b.accept((long rt) -> {
			try {
				rb.set(rt);
			} finally {
				r.run();
			}
		}), () -> { h.accept(fn.applyAsLong(ra.get(), rb.get())); });
	}
	
	/**
	 * Queues a pair of tasks for execution.
	 * 
	 * <p>{@link #fork(int)} should be queried before invoking this method.</p>
	 * <p>Both tasks must execute the provided {@link Consumer}s after completion.</p>
	 * <p>The default implementation delegates to {@link #queue(Consumer, Consumer, Runnable)}.</p>
	 * 
	 * @param <T> Task result type.
	 * @param <R> Return type.
	 * @param a First task.
	 * @param b Second task.
	 * @param h Consumer to be executed afterward both tasks are finished.
	 * @param fn Merge operator.
	 */
	default <T, R> void queue(Consumer<Consumer<T>> a, Consumer<Consumer<T>> b, Consumer<R> h, BiFunction<T, T, R> fn) {
		AtomicReference<T> ra = new AtomicReference<>();
		AtomicReference<T> rb = new AtomicReference<>();
		queue((Runnable r) -> a.accept((T rt) -> {
			try {
				ra.set(rt);
			} finally {
				r.run();
			}
		}),
		(Runnable r) -> b.accept((T rt) -> {
			try {
				rb.set(rt);
			} finally {
				r.run();
			}
		}), () -> { h.accept(fn.apply(ra.get(), rb.get())); });
	}

	/**
	 * Queues three tasks for execution.
	 * 
	 * <p>{@link #fork(int)} should be queried before invoking this method.</p>
	 * <p>All tasks must execute the provided {@link Consumer}s after completion.</p>
	 * <p>The default implementation delegates to {@link #queue(Consumer, Consumer, Runnable)}.</p>
	 * 
	 * @param <T> Task result type.
	 * @param <R> Return type.
	 * @param a First task.
	 * @param b Second task.
	 * @param c Third task.
	 * @param h Consumer to be executed afterward both tasks are finished.
	 * @param fn Merge operator.
	 */
	default <T, R> void queue(Consumer<Consumer<T>> a, Consumer<Consumer<T>> b, Consumer<Consumer<T>> c, Consumer<R> h, TriFunction<T, T, T, R> fn) {
		AtomicReference<T> ra = new AtomicReference<>();
		AtomicReference<T> rb = new AtomicReference<>();
		AtomicReference<T> rc = new AtomicReference<>();		
		queue((Runnable r) -> a.accept((T rt) -> {
				try {
					ra.set(rt);
				} finally {
					r.run();
				}
			}), (Runnable r) -> {
			queue((Runnable rh) -> b.accept((T rt) -> {
				try {
					rb.set(rt);
				} finally {
					rh.run();
				}
			}),
			(Runnable rh) -> c.accept((T rt) -> {
				try {
					rc.set(rt);
				} finally {
					rh.run();
				}
			}), r);
		},() -> { h.accept(fn.apply(ra.get(), rb.get(), rc.get())); });
	}
	
	/**
	 * Checks whether a work of given size should be split and queued instead of
	 * being executed by a non-parallel algorithm.
	 * 
	 * <p>This method must be invoked before every <code>execute</code> and should be invoked before
	 * every <code>queue</code>. 
	 * <p>If task size is less than or equal to one, always returns false.
	 * 
	 * @param taskSize Size of the work to be parallelized.
	 * @return True, if work can be parallelized.
	 */
	boolean fork(int taskSize);
	
	/**
	 * Creates an execution strategy.
	 * 
	 * <p>If concurrency is zero, {@link #LOCAL} is returned; otherwise an instance of
	 *  {@link ForkJoinPoolExecutionStrategy} is created with an associated pool which can be
	 *  accessed through {@link ForkJoinPoolExecutionStrategy#getPool()}
	 * 
	 * @param concurrency Concurrency number (base-2 logarithm of number of threads).
	 * @param workSize Minimal work size
	 * @return An execution strategy.
	 */
	static ExecutionStrategy create(int concurrency, int workSize) {
		return concurrency==0
				? LOCAL
				: new ForkJoinPoolExecutionStrategy(
						new ForkJoinPool(1<<concurrency, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true),
						ForkJoinPoolExecutionStrategy.defaultCondition(concurrency, workSize));
	}
	
}
