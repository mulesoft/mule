/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.functional;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.exception.AbstractMessagingExceptionStrategy;

/**
 * Restrict exceptions to debug log messages
 */
public class QuietExceptionStrategy extends AbstractMessagingExceptionStrategy {

  public QuietExceptionStrategy() {
    super(null);
  }

  @Override
  protected MuleEvent doHandleException(Exception e, MuleEvent event) {
    logger.debug("Ignoring", e);
    return event;
  }

  @Override
  protected void doLogException(Throwable t) {
    logger.debug("Ignoring", t);
  }

  public boolean isRedeliver() {
    return false;
  }
}
