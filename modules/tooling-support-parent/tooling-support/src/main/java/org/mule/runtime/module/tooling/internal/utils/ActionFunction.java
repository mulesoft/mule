/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.utils;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActionFunction {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActionFunction.class);

  private ActionFunction() {}

  public static <OutputType> OutputType actionCallWrapper(Supplier<OutputType> function, String service, String actionName) {
    if (!LOGGER.isInfoEnabled()) {
      return function.get();
    }

    LOGGER.info("Calling action '{}.{}'", service, actionName);
    long initialTime = System.currentTimeMillis();
    try {
      OutputType outputValue = function.get();
      long totalTimeSpent = System.currentTimeMillis() - initialTime;
      LOGGER.info("Calling action successfully: '{}.{}' [{}ms]", service, actionName, totalTimeSpent);
      return outputValue;
    } catch (Exception e) {
      long totalTimeSpent = System.currentTimeMillis() - initialTime;
      LOGGER.info("Calling action failure: '{}.{}' [{}ms]", service, actionName, totalTimeSpent);
      throw e;
    }
  }

}
