/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming.bytes;

import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;

import java.io.InputStream;

/**
 * Specialization of {@link CursorStreamProvider} which creates {@link CursorStreamProvider} instances
 * out of {@link InputStream} instances
 *
 * @since 4.0
 */
public interface CursorStreamProviderFactory extends CursorProviderFactory<InputStream> {

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
