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

/**
 * A decorator that turns any {@link CursorProvider} into an {@link IdentifiableCursorProvider}.
 * <p>
 * If the decoratee is already an {@link IdentifiableCursorProvider} or ({@link CursorProviderDecorator} of one), then this
 * decorator will yield the same id as the input identifiable provider. If this is not the case, an ID will be generaetd.mvn
 * -Dtest=ClosedCursorProviderTestCase test
 * <p>
 * Instances are to be created through the {@link #of(CursorProvider)} factory method.
 *
 * @param <T> the generic {@link Cursor} type as defined in {@link CursorProvider}
 * @since 4.4, 4.3.1, 4.2.3
 */
public abstract class IdentifiableCursorProviderDecorator<T extends Cursor> extends CursorProviderDecorator<T>
    implements IdentifiableCursorProvider<T> {

  private static final transient AtomicInteger ID_GENERATOR = new AtomicInteger(MIN_VALUE);

  private final int id;

  /**
   * Creates a new decorator for the given {@code cursorProvider}.
   * <p>
   * If the {@code cursorProvider} is already an {@link IdentifiableCursorProvider}, then the returned decorator will yield the
   * same id as the input {@code cursorProvider}. If {@code cursorProvider} is a {@link CursorProviderDecorator}, then the
   * {@link CursorProviderDecorator#getDelegate()} will be recursively be tested to find if any delegate in the chain is
   * identifiable. If such delegate is found, then the same {@code id} will be reused. Otherwise, one will be generated.
   *
   * @param cursorProvider the decoratee
   * @param <T>            the generic {@link Cursor} type
   * @return a new decorator.
   */
  public static <T extends Cursor> IdentifiableCursorProviderDecorator<T> of(CursorProvider<T> cursorProvider) {
    final CursorProvider<T> root = cursorProvider;
    if (cursorProvider instanceof IdentifiableCursorProviderDecorator) {
      return (IdentifiableCursorProviderDecorator<T>) cursorProvider;
    }
    Integer id = null;
    do {
      if (cursorProvider instanceof IdentifiableCursorProvider) {
        id = ((IdentifiableCursorProvider<T>) cursorProvider).getId();
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
