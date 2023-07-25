/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.splash;

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
