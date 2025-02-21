/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

public class EncryptionStrategyNotFoundExceptionTestCase {

  @Test
  public void testExceptionMessage() {
    String strategyName = "TestStrategy";
    EncryptionStrategyNotFoundException exception = new EncryptionStrategyNotFoundException(strategyName);

    assertThat(exception.getMessage(), containsString("There is no Encryption Strategy registered called 'TestStrategy'"));
  }

  @Test
  public void testExceptionMessageWithCause() {
    String strategyName = "AnotherStrategy";
    Throwable cause = new RuntimeException("Cause of failure");
    EncryptionStrategyNotFoundException exception = new EncryptionStrategyNotFoundException(strategyName, cause);

    assertThat(exception.getMessage(), containsString("There is no Encryption Strategy registered called 'AnotherStrategy'"));
    assertThat(exception.getCause(), is(cause));
  }
}
