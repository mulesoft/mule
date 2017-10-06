/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;

import java.util.List;

/**
 * A {@link CursorIteratorProvider} which is backed by a fixed {@link List}.
 * <p>
 * Notice that since the {@link #list} data is already fully loaded into memory, this kind of
 * defeats the purpose of the cursor provider. The purpose of this method is to provide a way to
 * bridge the given data with the {@link CursorIteratorProvider} abstraction. Possible use cases are
 * mainly deserialization and testing. <b>Think twice</b> before using this method. Most likely you're
 * doing something wrong.
 * <p>
 * Also consider that because the data is already in memory, the cursors will never buffer into disk.
 *
 * @since 4.0
 */
public class ListCursorIteratorProvider implements CursorIteratorProvider {

  private List<?> list;
  private boolean closed = false;

  public ListCursorIteratorProvider(List<?> list) {
    this.list = list;
  }

  @Override
  public CursorIterator openCursor() {
    return new ListCursorIterator(this, list);
  }

  @Override
  public void close() {
    closed = true;
  }

  @Override
  public void releaseResources() {
    list = null;
  }

  @Override
  public boolean isClosed() {
    return closed;
  }
}
