/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.xml.functional;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HideExceptions implements MessagingExceptionHandler {

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  public MuleEvent handleException(Exception exception, MuleEvent event) {
    logger.debug("Hiding exception: " + exception);
    logger.debug("(see config for test - some exceptions expected)");
    return null;
  }

}

