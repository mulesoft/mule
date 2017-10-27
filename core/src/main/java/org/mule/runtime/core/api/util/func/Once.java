/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util.func;

import static org.mule.runtime.core.internal.util.ConcurrencyUtils.withLock;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Utilities for scenarios in which a given task should be executed only once.
 *
 * @since 4.0
 */
public class Once {

  /**
   * Creates a new {@link RunOnce}
   *
   * @param runnable the delegate to be executed only once
   * @return a new instance
   */
  public static RunOnce of(CheckedRunnable runnable) {
    return new RunOnce(runnable);
  }

  /**
   * Creates a new {@link ConsumeOnce}
   *
   * @param consumer the delegate to be executed only once
   * @return a new instance
   */
  public static <T> ConsumeOnce<T> of(CheckedConsumer<T> consumer) {
    return new ConsumeOnce<>(consumer);
  }

  /**
   * Executes a given {@link CheckedConsumer} only once.
   * <p>
   * Once the {@link #consumeOnce()} method has been successfully executed, subsequent invocations to
   * such method will have no effect, even if the supplied value is different. Notice that the key word here
   * is {@code successfully}. If the method fails, each invocation to {@link #consumeOnce()} WILL run the delegate
   * until it completes successfully.
   * <p>
   * Instances are thread safe, which means that if two threads are competing for the first successful invocation, only
   * one will prevail and the other one will get a no-op execution.
   *
   * @since 4.0
   */
  public static class ConsumeOnce<T> extends AbstractOnce {

    private CheckedConsumer<T> consumer;

    private ConsumeOnce(CheckedConsumer<T> delegate) {
      consumer = v -> withLock(lock, () -> {
        if (!done) {
          delegate.accept(v);
          done = true;
          consumer = x -> {
          };
        }
      });
    }

    public void consumeOnce(T value) {
      consumer.accept(value);
    }
  }

  /**
   * Executes a given {@link CheckedRunnable} only once.
   * <p>
   * Once the {@link #runOnce()} method has been successfully executed, subsequent invocations to
   * such method will have no effect. Notice that the key word here is {@code successfully}. If the method fails,
   * each invocation to {@link #runOnce()} WILL run the delegate until it completes successfully.
   * <p>
   * Instances are thread safe, which means that if two threads are competing for the first successful invocation, only
   * one will prevail and the other one will get a no-op execution.
   *
   * @since 4.0
   */
  public static class RunOnce extends AbstractOnce {

    private CheckedRunnable runner;

    private RunOnce(CheckedRunnable delegate) {
      runner = () -> withLock(lock, () -> {
        if (!done) {
          delegate.run();
          done = true;
          runner = () -> {
          };
        }
      });
    }

    /**
     * Runs (or not) the delegate according to the behaviour described on the class javadoc
     */
    public void runOnce() {
      runner.run();
    }
  }

  private static abstract class AbstractOnce {

    protected final ReentrantLock lock = new ReentrantLock();
    protected volatile boolean done = false;
  }
}
