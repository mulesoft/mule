/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.log4j.internal;

/**
 * A functional interface that reconfigures a logging environment.
 *
 * @since 4.7.0
 */
interface LoggerReconfigurationAction {

  /**
   * @return if the logger was successfully reconfigured.
   */
  boolean reconfigure();
}

