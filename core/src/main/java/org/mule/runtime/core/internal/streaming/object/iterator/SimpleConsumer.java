/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.streaming.object.iterator;

import org.mule.runtime.core.api.streaming.iterator.AbstractConsumer;
import org.mule.runtime.core.api.streaming.iterator.Consumer;
import org.mule.runtime.core.api.streaming.iterator.Producer;

import java.util.NoSuchElementException;

/**
 * Basic implementation of {@link Consumer} that simply returns the objects returned by the underlying {@link Producer}
 * 
 * @since 3.5.0
 */
public class SimpleConsumer<T> extends AbstractConsumer<T, T> {

  private T next = null;

  public SimpleConsumer(Producer<T> producer) {
    super(producer);
  }

  @Override
  protected T doConsume() throws NoSuchElementException {
    T value;
    if (this.next != null) {
      value = this.next;
      this.next = null;
    } else {
      value = this.producer.produce();
    }

    return value;
  }

  @Override
  protected boolean checkConsumed() {
    if (this.next != null) {
      return false;
    } else {
      try {
        this.next = this.producer.produce();
      } catch (NoSuchElementException e) {
        this.next = null;
      }
    }

    return this.next == null;
  }

}
