/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.component.simple;

import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.component.simple.LogService;
import org.mule.runtime.core.api.lifecycle.Callable;
import org.mule.runtime.core.util.StringMessageUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>LogComponent</code> simply logs the content (or content length if it is a large message)
 */
public class LogComponent implements Callable, LogService {

  private static Logger logger = LoggerFactory.getLogger(LogComponent.class);

  public Object onCall(MuleEventContext context) throws Exception {
    String contents = context.getMessageAsString();
    String msg = "Message received in service: " + context.getFlowConstruct().getName();
    msg = StringMessageUtils.getBoilerPlate(msg + ". Content is: '" + StringMessageUtils.truncate(contents, 100, true) + "'");
    log(msg);
    return context.getMessage();
  }

  public void log(String message) {
    logger.info(message);
  }
}
