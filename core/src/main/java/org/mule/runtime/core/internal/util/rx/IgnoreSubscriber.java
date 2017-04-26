/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;

/**
 * Singleton {@link Subscriber} that request unlimited demand and ignores errors.
 * 
 * @param <T>
 */
public class IgnoreSubscriber<T> extends BaseSubscriber<T> {

  private static Subscriber INSTANCE = new IgnoreSubscriber<>();

  public static <T> Subscriber<T> get() {
    return new IgnoreSubscriber<>();
  }

  @Override
  protected void hookOnSubscribe(Subscription subscription) {
    subscription.request(Long.MAX_VALUE);

  }

  @Override
  protected void hookOnNext(T value) {

  }

  @Override
  protected void hookOnError(Throwable throwable) {
    // Ignore
  }
}
