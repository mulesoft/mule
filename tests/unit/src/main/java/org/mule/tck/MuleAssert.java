/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
