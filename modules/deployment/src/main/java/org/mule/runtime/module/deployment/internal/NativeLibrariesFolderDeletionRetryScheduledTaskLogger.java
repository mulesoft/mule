/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

/**
 * A Logger for the {@link NativeLibrariesFolderDeletionRetryScheduledTask} This is only used for 4.4.x.
 */
public interface NativeLibrariesFolderDeletionRetryScheduledTaskLogger {

  /**
   * Logs a message with DEBUG level.
   *
   * @param template   the template to log
   * @param parameters the parameters
   */
  void debug(String template, Object... parameters);

  /**
   * Logs a message with ERROR level.
   *
   * @param template   the template to log
   * @param parameters the parameters
   */
  void error(String template, Object... parameters);

  /**
   * Logs a message with INFO level.
   *
   * @param template   the template to log
   * @param parameters the parameters
   */
  void info(String template, Object... parameters);

  /**
   * Logs a message with WARN level.
   *
   * @param template   the template to log
   * @param parameters the parameters
   */
  void warn(String template, Object... parameters);
}
