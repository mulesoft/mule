/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object.factory;

import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.internal.streaming.CursorManager;
import org.mule.runtime.core.internal.streaming.object.iterator.StreamingIterator;
import org.mule.runtime.core.streaming.object.CursorIteratorProviderFactory;

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

  /**
   * {@inheritDoc}
   */
  @Override
  public final Object of(Event event, Iterator iterator) {
    if (iterator instanceof CursorIterator) {
      return ((CursorIterator) iterator).getProvider();
    }

    return resolve(iterator, event);
  }

  /**
   * Implementations should use this method to actually create the output value
   *
   * @param iterator the streaming iterator
   * @param event    the event on which streaming is happening
   */
  protected abstract Object resolve(Iterator iterator, Event event);

  /**
   * {@inheritDoc}
   *
   * @return {@code true} if the {@code value} is a {@link StreamingIterator}
   */
  @Override
  public boolean accepts(Object value) {
    return value instanceof StreamingIterator;
  }
}
