/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.bytes.ByteBufferManager;
import org.mule.runtime.core.internal.streaming.CursorManager;
import org.mule.runtime.core.internal.streaming.ManagedCursorProvider;
import org.mule.runtime.core.internal.streaming.MutableStreamingStatistics;
import org.mule.runtime.core.internal.streaming.bytes.DefaultByteStreamingManager;
import org.mule.runtime.core.internal.streaming.bytes.PoolingByteBufferManager;
import org.mule.runtime.core.internal.streaming.object.DefaultObjectStreamingManager;
import org.mule.runtime.core.api.streaming.bytes.ByteStreamingManager;
import org.mule.runtime.core.api.streaming.object.ObjectStreamingManager;

import javax.inject.Inject;

import org.slf4j.Logger;

public class DefaultStreamingManager implements StreamingManager, Initialisable, Disposable {

  private static final Logger LOGGER = getLogger(DefaultStreamingManager.class);

  private ByteBufferManager bufferManager;
  private ByteStreamingManager byteStreamingManager;
  private ObjectStreamingManager objectStreamingManager;
  private CursorManager cursorManager;
  private MutableStreamingStatistics statistics;
  private boolean initialised = false;

  @Inject
  private MuleContext muleContext;

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialise() throws InitialisationException {
    if (!initialised) {
      statistics = new MutableStreamingStatistics();
      cursorManager = new CursorManager(statistics);
      bufferManager = new PoolingByteBufferManager();
      byteStreamingManager = createByteStreamingManager();
      objectStreamingManager = createObjectStreamingManager();

      initialiseIfNeeded(byteStreamingManager, true, muleContext);
      initialiseIfNeeded(objectStreamingManager, true, muleContext);
      initialised = true;
    }
  }

  protected ByteStreamingManager createByteStreamingManager() {
    return new DefaultByteStreamingManager(bufferManager, this);
  }

  protected ObjectStreamingManager createObjectStreamingManager() {
    return new DefaultObjectStreamingManager(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose() {
    disposeIfNeeded(byteStreamingManager, LOGGER);
    disposeIfNeeded(objectStreamingManager, LOGGER);
    disposeIfNeeded(bufferManager, LOGGER);
    disposeIfNeeded(cursorManager, LOGGER);

    initialised = false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ByteStreamingManager forBytes() {
    return byteStreamingManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ObjectStreamingManager forObjects() {
    return objectStreamingManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CursorProvider manage(CursorProvider provider, CoreEvent creatorEvent) {
    if (provider instanceof ManagedCursorProvider) {
      return provider;
    }
    return cursorManager.manage(provider, creatorEvent);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StreamingStatistics getStreamingStatistics() {
    return statistics;
  }

  protected ByteBufferManager getBufferManager() {
    return bufferManager;
  }
}
