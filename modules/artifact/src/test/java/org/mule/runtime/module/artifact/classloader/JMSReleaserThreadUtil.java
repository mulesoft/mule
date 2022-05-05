/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public class JMSReleaserThreadUtil implements Runnable {

  private final Method startActiveMq;
  private final Object connection;

  private static final Logger LOGGER = LoggerFactory.getLogger(JMSReleaserThreadUtil.class);

  JMSReleaserThreadUtil(Method method, Object connection) {
    this.startActiveMq = method;
    this.connection = connection;
  }

  public void run() {
    try {
      startActiveMq.invoke(connection);
    } catch (Exception ex) {
      LOGGER.debug("There was an error trying to start connection to activeMQ in" + this.getClass().getCanonicalName());
    }
  }
}
