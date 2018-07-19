/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object.factory;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.iterator.StreamingIterator;
import org.mule.runtime.core.api.streaming.object.CursorIteratorProviderFactory;
import org.mule.runtime.core.internal.streaming.CursorManager;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.util.Iterator;

/**
 * Base implementation of {@link CursorIteratorProviderFactory} which contains all the base behaviour and template methods.
 * <p>
 * It interacts with the {@link CursorManager} in order to track all allocated resources and make sure they're properly disposed
 * of once they're no longer necessary.
 *
 * @since 4.0
 */
public abstract class AbstractCursorIteratorProviderFactory extends AbstractComponent
    implements CursorIteratorProviderFactory {

  private final StreamingManager streamingManager;

  public AbstractCursorIteratorProviderFactory(StreamingManager streamingManager) {
    this.streamingManager = streamingManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Object of(EventContext eventContext, Iterator iterator) {
    if (iterator instanceof CursorIterator) {
      return streamingManager.manage(((CursorIterator) iterator).getProvider(), eventContext);
    }

    Object value = resolve(iterator, eventContext);
    if (value instanceof CursorProvider) {
      value = streamingManager.manage((CursorProvider) value, eventContext);
    }

    return value;
  }

  @Override
  public Object of(CoreEvent event, Iterator value) {
    return of(((BaseEventContext) event.getContext()).getRootContext(), value);
  }

  /**
   * Implementations should use this method to actually create the output value
   *
   * @param iterator the streaming iterator
   * @param eventContext the root context of the event on which streaming is happening
   */
  protected abstract Object resolve(Iterator iterator, EventContext eventContext);

  /**
   * {@inheritDoc}
   *
   * @return {@code true} if the {@code value} is a {@link StreamingIterator}
   */
  @Override
  public boolean accepts(Object value) {
    return value instanceof Iterator;
  }
}
