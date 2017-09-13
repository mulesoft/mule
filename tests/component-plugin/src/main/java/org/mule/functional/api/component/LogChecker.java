/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

public interface LogChecker {

  /**
   * The method should check the log according to the rules defined in the implementation.
   *
   * @param logMessage the log message to check
   *
   * @throws Exception if the check fails
   */
  public void check(String logMessage) throws AssertionError;

}
