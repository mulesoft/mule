/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming.bytes;

import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.internal.streaming.bytes.SimpleByteBufferManager;
import org.mule.runtime.core.internal.streaming.bytes.factory.NullCursorStreamProviderFactory;

/**
 * An {@link ByteStreamingManager} which always returns a {@link NullCursorStreamProviderFactory}
 *
 * @since 4.5.0
 */
public class NullByteStreamingManager implements ByteStreamingManager {

  private final CursorStreamProviderFactory cursorStreamProviderFactory;

  public NullByteStreamingManager(StreamingManager streamingManager) {
    this.cursorStreamProviderFactory = new NullCursorStreamProviderFactory(new SimpleByteBufferManager(), streamingManager);
  }

  @Override
  public CursorStreamProviderFactory getInMemoryCursorProviderFactory(InMemoryCursorStreamConfig config) {
    return cursorStreamProviderFactory;
  }

  @Override
  public CursorStreamProviderFactory getNullCursorProviderFactory() {
    return cursorStreamProviderFactory;
  }

  @Override
  public CursorStreamProviderFactory getDefaultCursorProviderFactory() {
    return cursorStreamProviderFactory;
  }
}
