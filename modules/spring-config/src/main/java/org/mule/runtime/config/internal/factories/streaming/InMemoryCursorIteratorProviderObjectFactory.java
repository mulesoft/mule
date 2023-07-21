/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
