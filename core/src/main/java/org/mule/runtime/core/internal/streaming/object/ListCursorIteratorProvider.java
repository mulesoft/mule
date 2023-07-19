/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.streaming.object;

import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;

import java.util.List;

/**
 * A {@link CursorIteratorProvider} which is backed by a fixed {@link List}.
 * <p>
 * Notice that since the {@link #list} data is already fully loaded into memory, this kind of defeats the purpose of the cursor
 * provider. The purpose of this method is to provide a way to bridge the given data with the {@link CursorIteratorProvider}
 * abstraction. Possible use cases are mainly deserialization and testing. <b>Think twice</b> before using this method. Most
 * likely you're doing something wrong.
 * <p>
 * Also consider that because the data is already in memory, the cursors will never buffer into disk and will never be closed or
 * released. Resources are freed when this instance is garbage collected.
 *
 * @since 4.0
 */
public class ListCursorIteratorProvider implements CursorIteratorProvider {

  private List<?> list;

  public ListCursorIteratorProvider(List<?> list) {
    this.list = list;
  }

  @Override
  public CursorIterator openCursor() {
    return new ListCursorIterator(this, list);
  }

  @Override
  public void close() {}

  @Override
  public void releaseResources() {}

  @Override
  public boolean isClosed() {
    return false;
  }
}
