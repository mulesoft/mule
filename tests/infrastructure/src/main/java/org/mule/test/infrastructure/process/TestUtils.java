/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.infrastructure.process;

public class TestUtils {

  public static final String DEBUG_MODE_SYSTEM_PROPERTY_KEY = "debug";

  public static int getTimeout(int timeout) {
    if (System.getProperty(DEBUG_MODE_SYSTEM_PROPERTY_KEY) != null) {
      return 999999;
    }
    return timeout;
  }
}
