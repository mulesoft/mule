/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import java.util.concurrent.CountDownLatch;

import javax.annotation.Nonnull;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;

/**
 * {@link Subscriber} that just consumes a certain number of expected items, and allows for waiting until all items are consumed.
 *
 * @param <T> the type of items.
 */
class ItemsConsumer<T> extends BaseSubscriber<T> {

  private final CountDownLatch expectedItemsConsumedCountDownLatch;
  private Throwable error;

  /**
   * Creates the instance.
   *
   * @param numItemsExpected number of items expected.
   */
  ItemsConsumer(int numItemsExpected) {
    this.expectedItemsConsumedCountDownLatch = new CountDownLatch(numItemsExpected);
  }

  /**
   * Blocks the execution until the expected number of items have been consumed
   * <p>
   * If an error is received by this subscriber while waiting, the waiting thread is awakened and a {@link RuntimeException} is
   * raised with the received error as cause.
   * 
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  public void await() throws InterruptedException {
    expectedItemsConsumedCountDownLatch.await();
    if (error != null) {
      throw new RuntimeException(error);
    }
  }

  @Override
  protected void hookOnSubscribe(Subscription subscription) {
    subscription.request(expectedItemsConsumedCountDownLatch.getCount());
  }

  @Override
  protected void hookOnNext(@Nonnull T value) {
    expectedItemsConsumedCountDownLatch.countDown();
  }

  @Override
  protected void hookOnError(@Nonnull Throwable throwable) {
    error = throwable;
    while (expectedItemsConsumedCountDownLatch.getCount() > 0) {
      expectedItemsConsumedCountDownLatch.countDown();
    }
  }
}
