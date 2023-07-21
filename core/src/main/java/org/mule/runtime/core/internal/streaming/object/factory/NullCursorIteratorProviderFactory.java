/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.streaming.object.factory;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.streaming.StreamingManager;

import java.io.Closeable;
import java.util.Iterator;

public class NullCursorIteratorProviderFactory extends AbstractCursorIteratorProviderFactory {

  public NullCursorIteratorProviderFactory(StreamingManager streamingManager) {
    super(streamingManager);
  }

  @Override
  protected Object resolve(Iterator iterator, EventContext eventContext, ComponentLocation originatingLocation) {
    if (iterator instanceof Closeable) {
      streamingManager.manage((Closeable) iterator, eventContext);
    }

    return iterator;
  }
}
