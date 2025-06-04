/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.log4j.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;

public class MuleAlertingAsyncQueueFullPolicyTestCase {

  @Test
  public void callbackRegistration() {
    var policy = new MuleAlertingAsyncQueueFullPolicy();

    final var callback = mock(Consumer.class);
    policy.register(this.getClass().getClassLoader(), callback);

    policy.getRoute(1, Level.INFO);
    verify(callback).accept(this.getClass().getClassLoader());
  }
}
