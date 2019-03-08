/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object.factory;

import static org.mule.runtime.core.internal.streaming.StreamingStrategy.NON_REPEATABLE;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.internal.streaming.StreamingStrategy;

import java.util.Iterator;

public class NullCursorIteratorProviderFactory extends AbstractCursorIteratorProviderFactory implements HasStreamingStrategy {

  public NullCursorIteratorProviderFactory(StreamingManager streamingManager) {
    super(streamingManager);
  }

  @Override
  protected Object resolve(Iterator iterator, EventContext eventContext) {
    return iterator;
  }

  @Override
  public StreamingStrategy getStreamingStrategy() {
    return NON_REPEATABLE;
  }
}
