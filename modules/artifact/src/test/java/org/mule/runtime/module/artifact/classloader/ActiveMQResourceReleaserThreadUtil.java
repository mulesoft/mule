/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Method;
import org.slf4j.Logger;

/**
 * Created to be a Thread that start the connection to active mq in the test class {@link ActiveMQResourceReleaserTestCase}
 */
public class ActiveMQResourceReleaserThreadUtil implements Runnable {

  private final Method startActiveMq;
  private final Object connection;

  private static final Logger LOGGER = getLogger(ActiveMQResourceReleaserThreadUtil.class);

  ActiveMQResourceReleaserThreadUtil(Method method, Object connection) {
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
