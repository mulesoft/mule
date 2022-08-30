/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming.object;

import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.internal.streaming.object.factory.NullCursorIteratorProviderFactory;

/**
 * An {@link ObjectStreamingManager} which always returns a {@link NullCursorIteratorProviderFactory}
 *
 * @since 4.5.0
 */
public class NullObjectStreamingManager implements ObjectStreamingManager {

  private final CursorIteratorProviderFactory cursorIteratorProviderFactory;

  public NullObjectStreamingManager(StreamingManager streamingManager) {
    this.cursorIteratorProviderFactory = new NullCursorIteratorProviderFactory(streamingManager);
  }

  @Override
  public CursorIteratorProviderFactory getInMemoryCursorProviderFactory(InMemoryCursorIteratorConfig config) {
    return cursorIteratorProviderFactory;
  }

  @Override
  public CursorIteratorProviderFactory getNullCursorProviderFactory() {
    return cursorIteratorProviderFactory;
  }

  @Override
  public CursorIteratorProviderFactory getDefaultCursorProviderFactory() {
    return cursorIteratorProviderFactory;
  }
}
