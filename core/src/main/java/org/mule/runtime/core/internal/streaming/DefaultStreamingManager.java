/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.streaming.bytes.ByteStreamingManager;
import org.mule.runtime.core.internal.streaming.bytes.ByteStreamingManagerAdapter;
import org.mule.runtime.core.internal.streaming.bytes.DefaultByteStreamingManager;

import javax.inject.Inject;

import org.slf4j.Logger;

public class DefaultStreamingManager implements StreamingManagerAdapter, Initialisable, Disposable {

  private static final Logger LOGGER = getLogger(DefaultStreamingManager.class);

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
  public void dispose() {
    disposeIfNeeded(byteStreamingManager, LOGGER);
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
