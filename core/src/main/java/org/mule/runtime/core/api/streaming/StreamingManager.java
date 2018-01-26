/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.bytes.ByteStreamingManager;
import org.mule.runtime.core.api.streaming.object.ObjectStreamingManager;
import org.mule.runtime.core.internal.streaming.CursorContext;

import java.io.InputStream;

/**
 * Manages resources dedicated to perform streaming of bytes or objects, so that the runtime can keep track of them,
 * enforce policies and make sure that all resources are reclaimed once no longer needed.
 *
 * @since 4.0
 */
@NoImplement
public interface StreamingManager {

  /**
   * @return a delegate manager to be used when streaming bytes
   */
  ByteStreamingManager forBytes();

  /**
   * @return a delegate manager to be used when streaming objects
   */
  ObjectStreamingManager forObjects();

  /**
   * @return statistics about the ongoing streaming operations
   */
  StreamingStatistics getStreamingStatistics();

  /**
   * Becomes aware of the given {@code provider} and returns a replacement provider
   * which is managed by the runtime, allowing for automatic resource handling
   *
   * @param provider     the provider to be tracked
   * @param creatorEvent the event that created the provider
   * @return a {@link CursorContext}
   */
  CursorProvider manage(CursorProvider provider, CoreEvent creatorEvent);

  /**
   * Becomes aware of the given {@code inputStream} and makes sure it is closed
   * by the time the given {@code creatorEvent} (and all its parent events) are completed.
   * <p>
   * If {@code inputStream} is a {@link Cursor} then nothing happens. Use
   * {@link #manage(CursorProvider, CoreEvent)} for those cases.
   *
   * @param inputStream  the stream to track
   * @param creatorEvent the event on which the stream was created
   */
  void manage(InputStream inputStream, CoreEvent creatorEvent);

}
