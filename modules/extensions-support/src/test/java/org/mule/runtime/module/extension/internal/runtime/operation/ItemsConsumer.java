/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import java.util.concurrent.CountDownLatch;

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
  protected void hookOnNext(T value) {
    expectedItemsConsumedCountDownLatch.countDown();
  }

  @Override
  protected void hookOnError(Throwable throwable) {
    error = throwable;
    while (expectedItemsConsumedCountDownLatch.getCount() > 0) {
      expectedItemsConsumedCountDownLatch.countDown();
    }
  }
}
