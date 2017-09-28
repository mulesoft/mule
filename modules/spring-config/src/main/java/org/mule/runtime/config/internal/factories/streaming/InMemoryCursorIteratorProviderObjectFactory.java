/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories.streaming;

import org.mule.runtime.config.api.factories.streaming.AbstractCursorProviderObjectFactory;
import org.mule.runtime.core.api.streaming.object.CursorIteratorProviderFactory;
import org.mule.runtime.core.api.streaming.object.InMemoryCursorIteratorConfig;

public class InMemoryCursorIteratorProviderObjectFactory
    extends AbstractCursorProviderObjectFactory<CursorIteratorProviderFactory> {

  private final int initialBufferSize;
  private final int bufferSizeIncrement;
  private final int maxInMemoryInstances;

  public InMemoryCursorIteratorProviderObjectFactory(int initialBufferSize, int bufferSizeIncrement, int maxInMemoryInstances) {
    this.initialBufferSize = initialBufferSize;
    this.bufferSizeIncrement = bufferSizeIncrement;
    this.maxInMemoryInstances = maxInMemoryInstances;
  }

  @Override
  public CursorIteratorProviderFactory doGetObject() throws Exception {
    InMemoryCursorIteratorConfig config =
        new InMemoryCursorIteratorConfig(initialBufferSize, bufferSizeIncrement, maxInMemoryInstances);

    return streamingManager.forObjects().getInMemoryCursorProviderFactory(config);
  }
}
