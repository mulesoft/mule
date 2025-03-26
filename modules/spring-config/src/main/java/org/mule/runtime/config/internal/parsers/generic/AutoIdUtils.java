/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.parsers.generic;

import java.util.concurrent.atomic.AtomicInteger;

public final class AutoIdUtils {

  private AutoIdUtils() {
    // Nothing to do
  }

  public static final String ATTRIBUTE_ID = "id";
  public static final String ATTRIBUTE_NAME = "name";
  private static final AtomicInteger counter = new AtomicInteger(0);

  public static String uniqueValue(String value) {
    return value + "." + counter.incrementAndGet();
  }

}
