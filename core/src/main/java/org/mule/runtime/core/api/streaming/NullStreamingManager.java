/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.bytes.ByteStreamingManager;
import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.api.streaming.bytes.NullByteStreamingManager;
import org.mule.runtime.core.api.streaming.object.CursorIteratorProviderFactory;
import org.mule.runtime.core.api.streaming.object.NullObjectStreamingManager;
import org.mule.runtime.core.api.streaming.object.ObjectStreamingManager;
import org.mule.runtime.core.internal.streaming.NullStreamingStatistics;
import org.mule.runtime.core.internal.streaming.bytes.SimpleByteBufferManager;
import org.mule.runtime.core.internal.streaming.bytes.factory.NullCursorStreamProviderFactory;
import org.mule.runtime.core.internal.streaming.object.factory.NullCursorIteratorProviderFactory;

import java.io.Closeable;
import java.io.InputStream;

/**
 * {@link StreamingManager} which never manage the {@link CursorProvider}
 *
 * @since 4.5.0
 */
public class NullStreamingManager implements StreamingManager {

  public static final NullStreamingManager INSTANCE = new NullStreamingManager();

  private final ByteStreamingManager byteStreamingManager;
  private final ObjectStreamingManager objectStreamingManager;
  private final StreamingStatistics streamingStatistics;
  private final Pair<CursorStreamProviderFactory, CursorIteratorProviderFactory> pair;

  private NullStreamingManager() {
    this.byteStreamingManager = new NullByteStreamingManager(this);
    this.objectStreamingManager = new NullObjectStreamingManager(this);
    this.streamingStatistics = new NullStreamingStatistics();
    this.pair = new Pair<>(new NullCursorStreamProviderFactory(new SimpleByteBufferManager(), this),
                           new NullCursorIteratorProviderFactory(this));
  }

  @Override
  public ByteStreamingManager forBytes() {
    return byteStreamingManager;
  }

  @Override
  public ObjectStreamingManager forObjects() {
    return objectStreamingManager;
  }

  @Override
  public StreamingStatistics getStreamingStatistics() {
    return streamingStatistics;
  }

  @Override
  public CursorProvider manage(CursorProvider provider, EventContext creatorRootEventContext) {
    return provider;
  }

  @Override
  public CursorProvider manage(CursorProvider provider, CoreEvent creatorEvent) {
    return provider;
  }

  @Override
  public void manage(InputStream inputStream, EventContext creatorRootEventContext) {}

  @Override
  public void manage(Closeable closeable, EventContext creatorRootEventContext) {}

  @Override
  public void manage(InputStream inputStream, CoreEvent creatorEvent) {}

  @Override
  public Pair<CursorStreamProviderFactory, CursorIteratorProviderFactory> getPairFor(CursorProviderFactory provider) {
    return pair;
  }
}
