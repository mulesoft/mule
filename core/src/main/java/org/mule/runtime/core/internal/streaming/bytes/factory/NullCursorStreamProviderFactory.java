/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes.factory;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.internal.streaming.bytes.ByteBufferManager;

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
  public NullCursorStreamProviderFactory(ByteBufferManager bufferManager) {
    super(bufferManager);
  }

  /**
   * {@inheritDoc}
   *
   * @return the given {@code inputStream} wrapped in an {@link Either} instance
   */
  @Override
  protected Object resolve(InputStream inputStream, Event event) {
    return inputStream;
  }
}
