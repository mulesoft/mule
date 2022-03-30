/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import org.mule.runtime.api.exception.MuleException;

public class LocalErrorHandler extends ErrorHandler {

  @Override
  public void start() throws MuleException {
    if (logger.isDebugEnabled()) {
      logger.debug("In start of local error handler: {}", this);
    }
  }

  @Override
  public void stop() throws MuleException {
    if (logger.isDebugEnabled()) {
      logger.debug("In stop of local error handler: {}", this);
    }
  }
}
