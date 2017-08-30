/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.streaming.iterator;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Implementation of {@link Consumer} that obtains a {@link List} from a {@link Producer} and returns the elements one by one.
 * This implementation is not thread-safe.
 * 
 * @since 3.5.0
 */
public class ListConsumer<T> extends AbstractConsumer<T, List<T>> {

  private List<T> currentPage = null;
  private int index;
  private int pageSize;

  public ListConsumer(Producer<List<T>> producer) {
    super(producer);
    reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected T doConsume() throws NoSuchElementException {
    if (isConsumed()) {
      throw new NoSuchElementException();
    }

    T element = currentPage.get(index);
    index++;

    return element;
  }

  @Override
  protected boolean checkConsumed() {
    if (index >= pageSize) {
      loadNextPage();
      return pageSize == 0;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getSize() {
    return producer.getSize();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws IOException {
    super.close();
    currentPage = null;
  }

  private void reset() {
    index = 0;
    pageSize = currentPage == null ? 0 : currentPage.size();
  }

  public void loadNextPage() {
    currentPage = producer.produce();
    reset();
  }
}
