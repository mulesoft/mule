/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes.factory;

import static org.mule.runtime.core.api.functional.Either.right;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.internal.streaming.bytes.ByteStreamingManagerAdapter;
import org.mule.runtime.core.internal.streaming.bytes.CursorStreamProviderAdapter;
import org.mule.runtime.core.internal.streaming.bytes.SimpleByteBufferManager;

import java.io.InputStream;

/**
 * Implementation of {@link AbstractCursorStreamProviderFactory} which always returns
 * the original stream without creating any provider
 *
 * @since 4.0
 */
public class NullCursorStreamProviderFactory extends AbstractCursorStreamProviderFactory {

  /**
   * {@inheritDoc}
   */
  public NullCursorStreamProviderFactory(ByteStreamingManagerAdapter streamingManager) {
    super(streamingManager, new SimpleByteBufferManager());
  }

  /**
   * {@inheritDoc}
   *
   * @return the given {@code inputStream} wrapped in an {@link Either} instance
   */
  @Override
  protected Either<CursorStreamProviderAdapter, InputStream> resolve(InputStream inputStream, Event event) {
    return right(inputStream);
  }
}
