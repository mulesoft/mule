/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

import static java.lang.Math.toIntExact;
import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.internal.streaming.AbstractCursorIterator;

import java.io.IOException;
import java.util.List;

/**
 * A {@link CursorIterator} which is backed by a fixed {@link List}.
 * <p>
 * Notice that since the {@link #items} data is already fully loaded into memory, this kind of
 * defeats the purpose of the cursor provider. The purpose of this method is to provide a way to
 * bridge the given data with the {@link CursorIterator} abstraction. Possible use cases are
 * mainly deserialization and testing. <b>Think twice</b> before using this method. Most likely you're
 * doing something wrong.
 * <p>
 * Also consider that because the data is already in memory, the cursors will never buffer into disk.
 *
 * @since 4.0
 */
public class ListCursorIterator<T> extends AbstractCursorIterator<T> {

  private List<T> items;

  public ListCursorIterator(CursorIteratorProvider provider, List<T> items) {
    super(provider);
    this.items = items;
  }

  @Override
  protected T doNext(long position) {
    return items.get(toIntExact(position));
  }

  @Override
  protected void doClose() throws IOException {
    items = null;
  }

  @Override
  public boolean hasNext() {
    return getPosition() < items.size();
  }

  @Override
  public void release() {
    items = null;
  }

  @Override
  public int getSize() {
    return items.size();
  }
}
