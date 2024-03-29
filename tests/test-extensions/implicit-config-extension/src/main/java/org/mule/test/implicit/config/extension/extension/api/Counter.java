/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.implicit.config.extension.extension.api;

import java.util.concurrent.atomic.AtomicInteger;

public class Counter {

  private AtomicInteger value;

  public Counter(int value) {
    this.value = new AtomicInteger(value);
  }

  public int incrementAndGet() {
    return value.incrementAndGet();
  }

  public int getValue() {
    return value.get();
  }
}
