/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import org.junit.Assert;

/**
 * Custom assertions for Mule
 */
public class MuleAssert {

  public static void assertTrue(String message, java.util.concurrent.atomic.AtomicBoolean bool) {
    Assert.assertTrue(message, bool.get());
  }
}
