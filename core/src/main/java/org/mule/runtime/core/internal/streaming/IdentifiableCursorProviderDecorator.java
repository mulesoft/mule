/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static java.lang.Integer.MIN_VALUE;

import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class IdentifiableCursorProviderDecorator<T extends Cursor> extends CursorProviderDecorator<T>
    implements IdentifiableCursorProvider<T> {

  private static final transient AtomicInteger ID_GENERATOR = new AtomicInteger(MIN_VALUE);

  private final int id;

  public static <T extends Cursor> IdentifiableCursorProviderDecorator<T> of(CursorProvider<T> cursorProvider) {
    final CursorProvider<T> root = cursorProvider;
    Integer id = null;
    do {
      if (cursorProvider instanceof IdentifiableCursorProvider) {
        id = ((IdentifiableCursorProvider<T>) cursorProvider).getId();
        cursorProvider = CursorUtils.unwrap(cursorProvider);
        break;
      }

      if (cursorProvider instanceof CursorProviderDecorator) {
        cursorProvider = ((CursorProviderDecorator<T>) cursorProvider).getDelegate();
      }
    } while (cursorProvider instanceof CursorProviderDecorator);

    if (id == null) {
      id = ID_GENERATOR.incrementAndGet();
    }

    if (cursorProvider instanceof CursorStreamProvider) {
      return (IdentifiableCursorProviderDecorator<T>) new IdentifiableCursorStreamProviderDecorator(
                                                                                                    (CursorStreamProvider) root,
                                                                                                    id);
    } else {
      return (IdentifiableCursorProviderDecorator<T>) new IdentifiableCursorIteratorProviderDecorator(
                                                                                                      (CursorIteratorProvider) root,
                                                                                                      id);
    }
  }

  private IdentifiableCursorProviderDecorator(CursorProvider<T> delegate, int id) {
    super(delegate);
    this.id = id;
  }

  @Override
  public int getId() {
    return id;
  }

  private static class IdentifiableCursorStreamProviderDecorator extends IdentifiableCursorProviderDecorator<CursorStream>
      implements CursorStreamProvider {

    public IdentifiableCursorStreamProviderDecorator(CursorStreamProvider delegate, int id) {
      super(delegate, id);
    }
  }


  private static class IdentifiableCursorIteratorProviderDecorator extends IdentifiableCursorProviderDecorator<CursorIterator>
      implements CursorIteratorProvider {

    public IdentifiableCursorIteratorProviderDecorator(CursorIteratorProvider delegate, int id) {
      super(delegate, id);
    }
  }
}
