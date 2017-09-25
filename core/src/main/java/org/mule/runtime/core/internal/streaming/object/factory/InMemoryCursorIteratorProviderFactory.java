/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object.factory;

import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.iterator.StreamingIterator;
import org.mule.runtime.core.api.streaming.object.InMemoryCursorIteratorConfig;
import org.mule.runtime.core.internal.streaming.object.InMemoryCursorIteratorProvider;

import java.util.Iterator;

public class InMemoryCursorIteratorProviderFactory extends AbstractCursorIteratorProviderFactory {

  private final InMemoryCursorIteratorConfig config;

  /**
   * Creates a new instance
   *
   * @param config        the config for the generated providers
   */
  public InMemoryCursorIteratorProviderFactory(InMemoryCursorIteratorConfig config, StreamingManager streamingManager) {
    super(streamingManager);
    this.config = config;
  }

  /**
   * {@inheritDoc}
   *
   * @return a new {@link CursorIteratorProvider} wrapped in an {@link Either}
   */
  @Override
  protected Object resolve(Iterator iterator, CoreEvent event) {
    InMemoryCursorIteratorProvider inMemoryCursorIteratorProvider =
        new InMemoryCursorIteratorProvider((StreamingIterator) iterator, config);
    inMemoryCursorIteratorProvider.setAnnotations(getAnnotations());
    return inMemoryCursorIteratorProvider;
  }
}
