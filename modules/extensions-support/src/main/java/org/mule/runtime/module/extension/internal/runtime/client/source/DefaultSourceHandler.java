/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.source;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.client.source.SourceHandler;

import org.slf4j.Logger;

public class DefaultSourceHandler implements SourceHandler {

  private static final Logger LOGGER = getLogger(DefaultSourceHandler.class);

  private final SourceClient sourceClient;

  public DefaultSourceHandler(SourceClient sourceClient) {
    this.sourceClient = sourceClient;
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(sourceClient);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(sourceClient);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(sourceClient, LOGGER);
  }
}
