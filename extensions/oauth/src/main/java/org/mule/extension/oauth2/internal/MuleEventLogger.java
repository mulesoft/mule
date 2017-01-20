/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal;

import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.runtime.extension.api.runtime.operation.Result;

import org.slf4j.Logger;

/**
 * Helper class to reuse message content logging for troubleshooting using the logs.
 */
public class MuleEventLogger {

  private final Logger logger;

  public MuleEventLogger(Logger logger) {
    this.logger = logger;
  }

  /**
   * Logs the result message payload type and the payload.
   * 
   * @param result result to log.
   */
  public void logContent(Result<Object, HttpRequestAttributes> result) {
    logger.error("Message content type is " + result.getMediaType().get());
    logger.error("Message content is " + result.getOutput());
  }

}
