/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.streaming;

import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import org.mule.runtime.core.internal.streaming.object.iterator.ClosedConsumerException;
import org.mule.runtime.core.api.streaming.iterator.Consumer;
import org.mule.runtime.core.api.streaming.iterator.ListConsumer;
import org.mule.runtime.core.api.streaming.iterator.Producer;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import io.qameta.allure.Feature;

@SmallTest
@Feature(STREAMING)
public class ListConsumerTestCase {

  private static final int totalCount = 50;
  private static final int pageSize = 10;

  private Consumer<Integer> consumer;
  private Producer<List<Integer>> producer;

  private List<List<Integer>> pages;

  @Before
  public void setUp() {
    this.pages = this.getPages();
    this.producer = spy(new TestProducer());
    this.consumer = spy(new ListConsumer<>(this.producer));
  }

  @Test(expected = ClosedConsumerException.class)
  public void happyPath() throws Exception {
    List<Integer> elements = new ArrayList<Integer>();
    while (!this.consumer.isConsumed()) {
      elements.add(this.consumer.consume());
    }

    assertEquals(elements.size(), totalCount);
    assertTrue(this.consumer.isConsumed());

    for (List<Integer> page : pages) {
      assertTrue(elements.containsAll(page));
    }

    verify(this.consumer).close();
    verify(this.producer).close();

    this.consumer.consume();
  }

  @Test(expected = ClosedConsumerException.class)
  public void closeEarly() throws Exception {
    List<Integer> elements = new ArrayList<Integer>();

    for (int i = 0; i < pageSize; i++) {
      elements.add(this.consumer.consume());
    }

    this.consumer.close();
    assertEquals(pageSize, elements.size());
    assertTrue(elements.containsAll(this.pages.get(0)));
    assertTrue(this.consumer.isConsumed());
    this.consumer.consume();
  }

  @Test
  public void totalAvailable() {
    assertEquals(this.consumer.getSize(), totalCount);
  }

  @Test
  public void doubleClose() throws Exception {
    this.consumer.close();
    this.consumer.close();
  }

  private List<List<Integer>> getPages() {
    List<List<Integer>> pages = new ArrayList<>();
    List<Integer> page = new ArrayList<>();

    for (int i = 1; i <= totalCount; i++) {
      page.add(i);
      if (i % pageSize == 0) {
        pages.add(page);
        page = new ArrayList<>();
      }
    }

    return pages;
  }

  private class TestProducer implements Producer<List<Integer>> {

    private int index = 0;

    @Override
    public void close() throws IOException {}

    @Override
    public List<Integer> produce() {
      List<Integer> ret;

      if (this.index < pages.size()) {

        ret = pages.get(index);
        index++;
      } else {
        ret = new ArrayList<>();
      }

      return ret;
    }

    public int getSize() {
      return totalCount;
    }
  }
}
