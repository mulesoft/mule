/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object.factory;

import static org.mule.runtime.core.api.functional.Either.left;
import org.mule.runtime.api.streaming.objects.CursorIteratorProvider;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.internal.streaming.CursorManager;
import org.mule.runtime.core.internal.streaming.object.InMemoryCursorIteratorProvider;
import org.mule.runtime.core.internal.streaming.object.iterator.StreamingIterator;
import org.mule.runtime.core.streaming.objects.InMemoryCursorIteratorConfig;

import java.util.Iterator;

public class InMemoryCursorIteratorProviderFactory extends AbstractCursorIteratorProviderFactory {

  private final InMemoryCursorIteratorConfig config;

  /**
   * Creates a new instance
   *
   * @param cursorManager the manager which will track the produced providers.
   * @param config        the config for the generated providers
   */
  public InMemoryCursorIteratorProviderFactory(CursorManager cursorManager, InMemoryCursorIteratorConfig config) {
    super(cursorManager);
    this.config = config;
  }

  /**
   * {@inheritDoc}
   *
   * @return a new {@link CursorIteratorProvider} wrapped in an {@link Either}
   */
  @Override
  protected Either<CursorIteratorProvider, Iterator> resolve(Iterator iterator, Event event) {
    return left(new InMemoryCursorIteratorProvider((StreamingIterator) iterator, config));
  }
}
