/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.streaming;

import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.OBJECT_STREAMING;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import org.mule.runtime.core.internal.streaming.object.iterator.Consumer;
import org.mule.runtime.core.internal.streaming.object.iterator.ConsumerStreamingIterator;
import org.mule.runtime.core.internal.streaming.object.iterator.ListConsumer;
import org.mule.runtime.core.internal.streaming.object.iterator.Producer;
import org.mule.runtime.core.internal.streaming.object.iterator.StreamingIterator;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
@Features(STREAMING)
@Stories(OBJECT_STREAMING)
public class StreamingIteratorTestCase {

  private static final int PAGE_SIZE = 100;
  private static final int TOP = 3000;

  private PagingProvider<Object, String> delegate = new TestPagingProvider();

  @InjectMocks
  private Producer<List<String>> producer = new Producer<List<String>>() {

    @Override
    public int getSize() {
      return delegate.getTotalResults(new Object()).get();
    }

    @Override
    public void close() throws IOException {
      delegate.close();
    }

    @Override
    public List<String> produce() {
      return delegate.getPage(new Object());
    }
  };

  @Test
  @Description("Fully consume the iterator")
  public void iterateStreaming() throws Exception {
    StreamingIterator<String> it = this.newIterator();

    int count = 0;
    while (it.hasNext()) {
      it.next();
      count++;
    }

    assertEquals(count, TOP);
    it.close();
  }

  @Test
  @Description("A closed iterator doesn't have next")
  public void closedIterator() throws Exception {
    StreamingIterator<String> it = this.newIterator();
    it.close();
    Assert.assertFalse(it.hasNext());
  }

  @Test
  @Description("Iterator doesn't have next if consumer is closed")
  public void closedConsumer() throws Exception {
    Consumer<String> consumer = new ListConsumer<>(producer);
    StreamingIterator<String> it = new ConsumerStreamingIterator<>(consumer);
    consumer.close();
    assertThat(it.hasNext(), is(false));
  }

  @Test
  @Description("iterator has size")
  public void size() throws Exception {
    StreamingIterator<String> it = this.newIterator();
    assertThat(it.getSize(), is(TOP));
  }

  private StreamingIterator<String> newIterator() {
    Consumer<String> consumer = new ListConsumer<>(producer);
    return new ConsumerStreamingIterator<>(consumer);
  }

  public class TestPagingProvider implements PagingProvider<Object, String> {

    long counter = 0;

    public List<String> getPage(Object con) {
      if (counter < TOP) {
        List<String> page = new ArrayList<>(100);
        for (int i = 0; i < PAGE_SIZE; i++) {
          counter++;
          String value = RandomStringUtils.randomAlphabetic(5000);
          page.add(value);
        }

        return page;
      }

      return null;
    }

    public void close() throws IOException {}

    public Optional<Integer> getTotalResults(Object con) {
      return Optional.of(TOP);
    }
  }
}
