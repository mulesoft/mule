/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.nonimplicit.config.extension.extension.api;

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
