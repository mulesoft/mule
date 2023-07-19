/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.streaming.object.factory;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.object.InMemoryCursorIteratorConfig;
import org.mule.runtime.core.internal.streaming.object.InMemoryCursorIteratorProvider;

import java.util.Iterator;

public class InMemoryCursorIteratorProviderFactory extends AbstractCursorIteratorProviderFactory {

  private final InMemoryCursorIteratorConfig config;

  /**
   * Creates a new instance
   *
   * @param config the config for the generated providers
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
  protected Object resolve(Iterator iterator, EventContext eventContext, ComponentLocation originatingLocation) {
    return new InMemoryCursorIteratorProvider(iterator, config, originatingLocation, trackCursorProviderClose);
  }
}
