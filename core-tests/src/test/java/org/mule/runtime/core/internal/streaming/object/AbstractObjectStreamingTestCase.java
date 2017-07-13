/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.core.api.streaming.iterator.Consumer;
import org.mule.runtime.core.api.streaming.iterator.ConsumerStreamingIterator;
import org.mule.runtime.core.api.streaming.iterator.StreamingIterator;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public abstract class AbstractObjectStreamingTestCase extends AbstractMuleContextTestCase {

  protected final List<Object> data;

  public AbstractObjectStreamingTestCase(int dataSize) {
    data = new ArrayList<>(dataSize);
    for (int i = 0; i < dataSize; i++) {
      data.add(createDataInstance());
    }
  }

  protected Object createDataInstance() {
    return randomAlphabetic(10);
  }

  protected <T> StreamingIterator<T> toStreamingIterator(List<T> data) {
    return new ConsumerStreamingIterator<>(new TestConsumer(data));
  }

  protected <T> void checkEquals(Collection<T> data, CursorIterator<T> cursor) {
    assertThat(data.size(), is(cursor.getSize()));
    int i = 0;
    for (T collectionItem : data) {
      assertThat("Cursor exhausted at position " + i, cursor.hasNext(), is(true));
      T cursorItem = cursor.next();
      assertThat("Unequal items at position " + i, cursorItem, equalTo(collectionItem));
      i++;
    }
  }

  protected <T> void checkEquals(List<T> actual, List<T> expected) {
    assertThat(actual.size(), is(expected.size()));
    assertThat(actual, equalTo(expected));
  }

  protected <T> List<T> read(CursorIterator<T> cursor, int count) {
    List<T> list = new ArrayList<>(count);
    for (int i = 0; cursor.hasNext() && i < count; i++) {
      list.add(cursor.next());
    }

    return list;
  }

  private class TestConsumer<T> implements Consumer<T> {

    private final Iterator<T> delegate;
    private final int size;

    private TestConsumer(Collection<T> collection) {
      this.delegate = collection.iterator();
      size = collection.size();
    }

    @Override
    public T consume() throws NoSuchElementException {
      return delegate.next();
    }

    @Override
    public boolean isConsumed() {
      return !delegate.hasNext();
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public int getSize() {
      return size;
    }
  }
}
