/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.streaming;

import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import org.mule.runtime.core.internal.streaming.object.iterator.ClosedConsumerException;
import org.mule.runtime.core.api.streaming.iterator.Consumer;
import org.mule.runtime.core.api.streaming.iterator.Producer;
import org.mule.runtime.core.internal.streaming.object.iterator.SimpleConsumer;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import io.qameta.allure.Feature;

@SmallTest
@Feature(STREAMING)
public class SimpleConsumerTestCase {

  private Producer<String> producer;
  private Consumer<String> consumer;
  private final Set<String> values = new HashSet<String>(Arrays.asList("apple", "banana", "kiwi"));

  @Before
  public void setUp() throws Exception {
    this.producer = new TestProducer();
    this.consumer = new SimpleConsumer<String>(this.producer);
  }

  @Test
  public void happyPath() throws Exception {
    assertFalse(this.consumer.isConsumed());
    while (!this.consumer.isConsumed()) {
      assertTrue(this.values.contains(this.consumer.consume()));
    }

    assertTrue(this.consumer.isConsumed());
  }

  @Test(expected = ClosedConsumerException.class)
  public void closeEarly() throws Exception {
    assertFalse(this.consumer.isConsumed());
    this.consumer.consume();
    this.producer.close();
    assertTrue(this.consumer.isConsumed());
    assertNull(this.consumer.consume());
  }

  @Test
  public void totalAvailable() {
    assertEquals(this.consumer.getSize(), this.values.size());
  }

  @Test
  public void doubleClose() throws Exception {
    this.consumer.close();
    this.consumer.close();
  }

  private class TestProducer implements Producer<String> {

    private boolean closed = false;
    private final Iterator<String> iterator;

    private TestProducer() {
      this.iterator = values.iterator();
    }

    @Override
    public String produce() {
      if (this.closed) {
        return null;
      }

      String value = this.iterator.next();
      if (value == null) {
        try {
          this.close();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }

      return value;
    }

    @Override
    public int getSize() {
      return values.size();
    }

    @Override
    public void close() throws IOException {
      this.closed = true;
    }
  }

}
