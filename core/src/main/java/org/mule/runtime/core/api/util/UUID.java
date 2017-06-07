/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

/**
 * <code>UUID</code> Generates a UUID using the <a href="http://johannburkard.de/software/uuid/">Johann Burkard UUID Library</a>.
 * In our performance tests we found this to be the implementation of type 1 UUID that was most performant in high concurrency
 * scenarios.
 */
// @ThreadSafe
public final class UUID {

  private UUID() {
    // no go
  }

  public static String getUUID() {
    return new com.eaio.uuid.UUID().toString();
  }
}
