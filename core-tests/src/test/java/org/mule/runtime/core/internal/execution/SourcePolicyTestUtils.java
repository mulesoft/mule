/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.api.util.concurrent.Latch;

import java.util.function.Consumer;

import org.mockito.stubbing.Answer;

/**
 * Utilities for testing policies applied to message sources
 *
 * @since 4.3.0
 */
public final class SourcePolicyTestUtils {

  private SourcePolicyTestUtils() {}

  /**
   * Generates a {@link CompletableCallback} that will block execution until it is completed and feeds it into the
   * provided {@code callbackConsumer}.
   * <p>
   * If the callback completes normally, this method returns the completion value. If it completes exceptionally, then the
   * obtained {@link Throwable} is thrown
   *
   * @param callbackConsumer a consumer that will use the generated callback
   * @param <T>              the callback's generic type
   * @return the completion value
   * @throws Throwable if the callback completes exceptionally
   */
  public static <T> T block(Consumer<CompletableCallback<T>> callbackConsumer) throws Throwable {
    Reference<T> valueReference = new Reference<>();
    Reference<Throwable> exceptionReference = new Reference<>();
    Latch latch = new Latch();

    CompletableCallback<T> callback = new CompletableCallback<T>() {

      @Override
      public void complete(T value) {
        valueReference.set(value);
        latch.release();
      }

      @Override
      public void error(Throwable e) {
        exceptionReference.set(e);
        latch.release();
      }
    };

    callbackConsumer.accept(callback);
    latch.await();

    if (exceptionReference.get() != null) {
      throw exceptionReference.get();
    }

    return valueReference.get();
  }

  /**
   * Returns a Mockito {@link Answer} that operates into a {@link CompletableCallback} argument assumed to be on arg index 2.
   *
   * @param callbackConsumer the consumer that will process the callback
   * @param <T>              the generic type of the answer
   * @return a Mockito {@link Answer}
   */
  public static <T> Answer<T> onCallback(Consumer<CompletableCallback<T>> callbackConsumer) {
    return onCallback(callbackConsumer, 2);
  }

  /**
   * Returns a Mockito {@link Answer} that operates into a {@link CompletableCallback} argument, which will be located on the
   * given {@code argIndex}.
   *
   * @param callbackConsumer the consumer that will process the callback
   * @param <T>              the generic type of the answer
   * @return a Mockito {@link Answer}
   */
  public static <T> Answer<T> onCallback(Consumer<CompletableCallback<T>> callbackConsumer, int argIndex) {
    return inv -> {
      callbackConsumer.accept(inv.getArgument(argIndex));
      return null;
    };
  }
}
