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

import java.util.concurrent.atomic.AtomicInteger;

public class IdentifiableCursorProviderDecorator<T extends Cursor> extends CursorProviderDecorator<T>
    implements IdentifiableCursorProvider<T> {

  private static final transient AtomicInteger ID_GENERATOR = new AtomicInteger(MIN_VALUE);

  private final int id;

  public static <T extends Cursor> IdentifiableCursorProviderDecorator<T> of(CursorProvider<T> cursorProvider) {
    final CursorProvider<T> root = cursorProvider;
    do {
      if (cursorProvider instanceof IdentifiableCursorProvider) {
        return new IdentifiableCursorProviderDecorator<>(root, ((IdentifiableCursorProvider<T>) cursorProvider).getId());
      }

      if (cursorProvider instanceof CursorProviderDecorator) {
        cursorProvider = ((CursorProviderDecorator<T>) cursorProvider).getDelegate();
      }
    } while (cursorProvider instanceof CursorProviderDecorator);

    return new IdentifiableCursorProviderDecorator<>(root, ID_GENERATOR.incrementAndGet());
  }

  private IdentifiableCursorProviderDecorator(CursorProvider<T> delegate, int id) {
    super(delegate);
    this.id = id;
  }

  @Override
  public int getId() {
    return id;
  }
}
