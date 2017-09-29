/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.connector;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PetStoreRetryPolicyProviderConnectionTestCase extends AbstractExtensionFunctionalTestCase {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  public PetStoreRetryPolicyProviderConnectionTestCase() {}

  @Override
  protected String getConfigFile() {
    return "petstore-retry-policy.xml";
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  @Test
  public void retryPolicyExhaustedDueToInvalidConnectionExecutingOperation() throws Exception {
    exception.expectCause(is(instanceOf(ConnectionException.class)));
    runFlow("fail-operation-with-connection-exception");
  }

  @Test
  public void retryPolicyExhaustedDueToInvalidConnectionAtValidateTime() throws Exception {
    exception.expectCause(is(instanceOf(ConnectionException.class)));
    runFlow("fail-connection-validation");
  }

  @Test
  public void retryPolicyNotExecutedDueToNotConnectionExceptionWithException() throws Exception {
    exception.expectCause(is(instanceOf(Throwable.class)));
    runFlow("fail-operation-with-not-handled-exception");
  }

  @Test
  public void retryPolicyNotExecutedDueToNotConnectionExceptionWithThrowable() throws Throwable {
    exception.expectCause(is(instanceOf(Throwable.class)));
    runFlow("fail-operation-with-not-handled-throwable");
  }
}
