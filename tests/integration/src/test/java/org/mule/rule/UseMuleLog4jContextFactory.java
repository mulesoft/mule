/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.rule;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.launcher.log4j2.MuleLog4jContextFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.junit.rules.ExternalResource;

/**
 * Allows tests to use the mule logging infrastructure without initializing a {@link MuleContext}.
 */
public class UseMuleLog4jContextFactory extends ExternalResource {

  private static MuleLog4jContextFactory muleLog4jContextFactory = new MuleLog4jContextFactory();

  private LoggerContextFactory originalLog4jContextFactory;

  @Override
  protected void before() throws Throwable {
    originalLog4jContextFactory = LogManager.getFactory();
    LogManager.setFactory(muleLog4jContextFactory);
  }

  @Override
  protected void after() {
    // We can safely force a removal of the old logger contexts instead of waiting for the reaper thread to do it.
    ((MuleLog4jContextFactory) LogManager.getFactory()).dispose();
    LogManager.setFactory(originalLog4jContextFactory);
  }
}
