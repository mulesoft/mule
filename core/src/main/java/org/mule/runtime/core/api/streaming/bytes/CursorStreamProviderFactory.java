/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming.bytes;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.internal.streaming.bytes.SimpleByteBufferManager;
import org.mule.runtime.core.internal.streaming.bytes.factory.NullCursorStreamProviderFactory;

import java.io.InputStream;

/**
 * Specialization of {@link CursorStreamProvider} which creates {@link CursorStreamProvider} instances out of {@link InputStream}
 * instances
 *
 * @since 4.0
 */
@NoImplement
public interface CursorStreamProviderFactory extends CursorProviderFactory<InputStream> {

  /**
   * @param streamingManager the {@link StreamingManager} to handle the {@link InputStream}s.
   * @return a {@link CursorStreamProviderFactory} which always returns the original stream without creating any provider.
   * @since 4.6
   */
  static CursorStreamProviderFactory nullCursorStreamProviderFactory(StreamingManager streamingManager) {
    return new NullCursorStreamProviderFactory(new SimpleByteBufferManager(), streamingManager);
  }

  /**
   * {@inheritDoc}
   *
   * @return {@code true} if the value is an {@link InputStream}
   */
  @Override
  default boolean accepts(Object value) {
    return value instanceof InputStream;
  }
}
