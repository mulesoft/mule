/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object.factory;

import static org.mule.runtime.core.api.functional.Either.left;
import org.mule.runtime.api.streaming.objects.CursorIterator;
import org.mule.runtime.api.streaming.objects.CursorIteratorProvider;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.internal.streaming.CursorManager;
import org.mule.runtime.core.internal.streaming.CursorProviderHandle;
import org.mule.runtime.core.internal.streaming.object.iterator.ConsumerIterator;
import org.mule.runtime.core.streaming.objects.CursorIteratorProviderFactory;

import java.util.Iterator;

/**
 * Base implementation of {@link CursorIteratorProviderFactory} which contains all the base behaviour and template
 * methods.
 * <p>
 * It interacts with the {@link CursorManager} in order to track all allocated resources and make
 * sure they're properly disposed of once they're no longer necessary.
 *
 * @since 4.0
 */
public abstract class AbstractCursorIteratorProviderFactory implements CursorIteratorProviderFactory {

  private final CursorManager cursorManager;

  /**
   * Creates a new instance
   *
   * @param cursorManager the manager which will track the produced providers.
   */
  protected AbstractCursorIteratorProviderFactory(CursorManager cursorManager) {
    this.cursorManager = cursorManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Either<CursorIteratorProvider, Iterator> of(Event event, Iterator iterator) {
    if (iterator instanceof CursorIterator) {
      return left((CursorIteratorProvider) ((CursorIterator) iterator).getProvider());
    }

    Either<CursorIteratorProvider, Iterator> value = resolve(iterator, event);
    return value.mapLeft(provider -> {
      CursorProviderHandle handle = cursorManager.track(provider, event);
      return provider;
    });
  }

  /**
   * Implementations should use this method to actually create the output value
   *
   * @param iterator the streaming iterator
   * @param event the event on which streaming is happening
   * @return
   */
  protected abstract Either<CursorIteratorProvider, Iterator> resolve(Iterator iterator, Event event);

  /**
   * {@inheritDoc}
   * @return {@code true} if the {@code value} is a {@link ConsumerIterator}
   */
  @Override
  public boolean accepts(Object value) {
    return value instanceof ConsumerIterator;
  }
}
