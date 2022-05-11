/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import static org.mule.runtime.core.internal.context.DefaultMuleContext.currentMuleContext;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.util.queue.DefaultQueueConfiguration;
import org.mule.tck.core.util.queue.QueueStoreTestCase;

import java.io.ObjectInputStream;
import java.io.Serializable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DualRandomAccessFileQueueStoreTestCase extends QueueStoreTestCase {

  @Before
  public void setUp() {
    currentMuleContext.set(muleContext);
  }

  @After
  public void teardown() {
    currentMuleContext.set(null);
  }

  @Override
  protected QueueStore createQueueInfoDelegate(int capacity, MuleContext muleContext) {
    return new DefaultQueueStore("testQueue", muleContext, new DefaultQueueConfiguration(capacity, true));
  }

  @Test
  public void containsDoesNotLoadEverythingInMemory() throws Exception {
    final DefaultQueueStore queue = (DefaultQueueStore) createQueue();
    queue.offer(new CounterClass(1), 0, 10);
    final CounterClass counterClassToSearch = new CounterClass(2);
    queue.offer(counterClassToSearch, 0, 10);
    queue.offer(new CounterClass(3), 0, 10);
    CounterClass.clearNumberOfInstances();
    queue.contains(counterClassToSearch);
    assertThat(CounterClass.numberOfInstances, is(2));
  }

  public static class CounterClass implements Serializable {

    private static final long serialVersionUID = -6160255403847961698L;

    public static int numberOfInstances = 0;
    private final int value;

    public CounterClass(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }

    public static void clearNumberOfInstances() {
      numberOfInstances = 0;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      CounterClass that = (CounterClass) o;

      if (value != that.value) {
        return false;
      }

      return true;
    }

    private void readObject(ObjectInputStream in) throws Exception {
      numberOfInstances++;
      in.defaultReadObject();
    }

    @Override
    public int hashCode() {
      return value;
    }
  }
}
