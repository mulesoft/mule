/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.bytes.ByteBufferManager;
import org.mule.runtime.core.api.streaming.bytes.ByteStreamingManager;
import org.mule.runtime.core.api.streaming.object.ObjectStreamingManager;
import org.mule.runtime.core.internal.streaming.CursorManager;
import org.mule.runtime.core.internal.streaming.FunkyCursorManager;
import org.mule.runtime.core.internal.streaming.ManagedCursorProvider;
import org.mule.runtime.core.internal.streaming.AtomicStreamingStatistics;
import org.mule.runtime.core.internal.streaming.bytes.DefaultByteStreamingManager;
import org.mule.runtime.core.internal.streaming.bytes.PoolingByteBufferManager;
import org.mule.runtime.core.internal.streaming.object.DefaultObjectStreamingManager;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.io.InputStream;

import javax.inject.Inject;

import org.slf4j.Logger;

@NoExtend
public class DefaultStreamingManager implements StreamingManager, Initialisable, Disposable {

  private static final Logger LOGGER = getLogger(DefaultStreamingManager.class);

  private ByteBufferManager bufferManager;
  private ByteStreamingManager byteStreamingManager;
  private ObjectStreamingManager objectStreamingManager;
  private CursorManager cursorManager;
  private AtomicStreamingStatistics statistics;
  private boolean initialised = false;

  private Scheduler allocationScheduler;

  @Inject
  private MuleContext muleContext;

  @Inject
  private SchedulerService schedulerService;

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialise() throws InitialisationException {
    if (!initialised) {
      statistics = new AtomicStreamingStatistics();
      allocationScheduler =
          schedulerService.ioScheduler(muleContext.getSchedulerBaseConfig().withName("StreamingManager-allocate"));
      cursorManager = new FunkyCursorManager(statistics);
      bufferManager = new PoolingByteBufferManager(allocationScheduler);
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
    allocationScheduler.stop();

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
  public CursorProvider manage(CursorProvider provider, EventContext creatorEventContext) {
    if (provider instanceof ManagedCursorProvider) {
      return provider;
    }
    return cursorManager.manage(provider, (BaseEventContext) creatorEventContext);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void manage(InputStream stream, EventContext creatorEventContext) {
    if (stream instanceof Cursor) {
      return;
    }

    ((BaseEventContext) creatorEventContext).onTerminated((response, throwable) -> closeQuietly(stream));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CursorProvider manage(CursorProvider provider, CoreEvent creatorEvent) {
    return manage(provider, ((BaseEventContext) creatorEvent.getContext()).getRootContext());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void manage(InputStream stream, CoreEvent creatorEvent) {
    manage(stream, ((BaseEventContext) creatorEvent.getContext()).getRootContext());
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
