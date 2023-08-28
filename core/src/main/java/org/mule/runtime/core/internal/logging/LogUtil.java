/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.logging;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

/**
 * Internal logging util.
 *
 * @since 4.2
 */
public class LogUtil {

  public static Logger LOGGER = getLogger(LogUtil.class);

  /**
   * Logs information relevant to the user and time agnostic.
   * <p/>
   * Commonly used to startup of components to detail used configuration.
   *
   * @param content content to be logged
   */
  public static void log(String content) {
    LOGGER.info(content);
  }

}
