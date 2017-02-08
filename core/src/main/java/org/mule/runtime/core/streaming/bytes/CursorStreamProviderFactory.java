/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.streaming.bytes;

import org.mule.runtime.api.streaming.CursorStreamProvider;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.functional.Either;

import java.io.InputStream;

/**
 * Creates instances of {@link CursorStreamProvider}
 *
 * @since 4.0
 */
public interface CursorStreamProviderFactory {

  /**
   * Optionally creates a new {@link CursorStreamProvider} to buffer the given {@code inputStream}.
   * <p>
   * Implementations might resolve that the given stream is/should not be buffered and thus
   * it will return the same given stream. In that case, the stream will be unaltered.
   *
   * @param event       the event on which buffering is talking place
   * @param inputStream the stream to be buffered
   * @return {@link Either} a {@link CursorStreamProvider} or the same given {@code inputStream}
   */
  Either<CursorStreamProvider, InputStream> of(Event event, InputStream inputStream);
}
