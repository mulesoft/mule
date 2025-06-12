/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.source;

import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.client.source.SourceHandler;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

/**
 * Default implementation of {@link SourceHandler}
 *
 * @since 4.5.0
 */
public class DefaultSourceHandler implements SourceHandler {

  private static final Logger LOGGER = getLogger(DefaultSourceHandler.class);

  private final SourceClient sourceClient;
  private final Runnable afterDisposeAction;
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final AtomicBoolean disposed = new AtomicBoolean(false);

  public DefaultSourceHandler(SourceClient sourceClient, Runnable afterDisposeAction) {
    this.sourceClient = sourceClient;
    this.afterDisposeAction = afterDisposeAction;
  }

  @Override
  public void start() throws MuleException {
    assertNotDisposed();
    if (started.compareAndSet(false, true)) {
      startIfNeeded(sourceClient);
    }
  }

  @Override
  public void stop() throws MuleException {
    assertNotDisposed();
    if (started.compareAndSet(true, false)) {
      stopIfNeeded(sourceClient);
    }
  }

  @Override
  public void dispose() {
    if (disposed.compareAndSet(false, true)) {
      disposeIfNeeded(sourceClient, LOGGER);
      try {
        afterDisposeAction.run();
      } catch (Exception e) {
        LOGGER.atError().setCause(e).log("Exception executing afterDisposeAction: " + e.getMessage());
      }
    }
  }

  private void assertNotDisposed() {
    checkState(!disposed.get(), "This source has already been disposed");
  }
}
