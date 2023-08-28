/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.util;


import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import org.mule.runtime.api.util.Preconditions;

import org.junit.Test;

public class PreconditionsTest {

  @Test
  public void validateCheckArgumentThrowsAnExceptionWhenConditionIsFalse() {
    try {
      Preconditions.checkArgument(false, "MyMessage");
      fail("IllegalArgumentException must be thrown");
    } catch (IllegalArgumentException e) {
      assertEquals("MyMessage", e.getMessage());
    }
  }

  @Test
  public void validateCheckArgument() {
    try {
      Preconditions.checkArgument(true, "MyMessage");
    } catch (IllegalArgumentException e) {
      fail("IllegalArgumentException must not be thrown when condition is true");
    }
  }
}
