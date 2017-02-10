/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.internal.streaming.bytes.ByteStreamingManagerAdapter;
import org.mule.runtime.core.internal.streaming.bytes.DefaultByteStreamingManager;
import org.mule.runtime.core.streaming.bytes.ByteStreamingManager;

import javax.inject.Inject;

public class DefaultStreamingManager implements StreamingManagerAdapter, Initialisable, Stoppable {

  @Inject
  private SchedulerService schedulerService;

  @Inject
  private MuleContext muleContext;

  private ByteStreamingManagerAdapter byteStreamingManager;
  private boolean initialised = false;

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialise() throws InitialisationException {
    if (!initialised) {
      byteStreamingManager = new DefaultByteStreamingManager(schedulerService.ioScheduler(), muleContext);
      initialised = true;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() throws MuleException {
    stopIfNeeded(byteStreamingManager);
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
  public void success(Event event) {
    byteStreamingManager.success(event);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void error(Event event) {
    byteStreamingManager.error(event);
  }
}
