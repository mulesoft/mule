/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.connector;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.petstore.extension.FailingPetStoreSource.connectionException;
import static org.mule.test.petstore.extension.FailingPetStoreSource.executor;

import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.FailingPetStoreSource;
import org.mule.test.petstore.extension.PetStoreConnector;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PetStoreSourceRetryPolicyProviderTestCase extends AbstractExtensionFunctionalTestCase {

  public static final int TIMEOUT_MILLIS = 10000;
  public static final int POLL_DELAY_MILLIS = 50;

  @Rule
  public ExpectedException exception = none();

  @Override
  protected String getConfigFile() {
    return "petstore-source-retry-policy.xml";
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    PetStoreConnector.clearTimesStarted();
    FailingPetStoreSource.failedDueOnException = false;

    super.doSetUpBeforeMuleContextCreation();
  }

  @Override
  protected void doTearDown() throws Exception {
    PetStoreConnector.clearTimesStarted();
    FailingPetStoreSource.failedDueOnException = false;
    if (executor != null) {
      executor.shutdownNow();
    }

    super.doTearDown();
  }

  @Test
  public void retryPolicySourceFailOnStart() throws Exception {
    exception.expect(RetryPolicyExhaustedException.class);
    exception.expectCause(sameInstance(connectionException));
    try {
      startFlow("source-fail-on-start");
    } catch (Exception e) {
      probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS, () -> {
        assertThat(PetStoreConnector.getTimesStarted(), is(2));
        return true;
      });
      throw e;
    }
  }

  @Test
  public void retryPolicySourceFailWithConnectionException() throws Exception {
    startFlow("source-fail-with-connection-exception");
    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS, () -> {
      assertThat(PetStoreConnector.getTimesStarted(), is(3));
      return true;
    });
  }

  @Test
  public void retryPolicySourceFailOnStartFallbackToConnection() throws Exception {
    exception.expect(RetryPolicyExhaustedException.class);
    exception.expectCause(sameInstance(connectionException));
    try {
      startFlow("source-fail-on-start-fallback-to-connection");
    } catch (Exception e) {
      probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS, () -> {
        assertThat(PetStoreConnector.getTimesStarted(), is(2));
        return true;
      });
      throw e;
    }
  }

  @Test
  public void retryPolicySourceFailWithConnectionExceptionFallbackToConnection() throws Exception {
    startFlow("source-fail-with-connection-exception-fallback-to-connection");
    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS, () -> {
      assertThat(PetStoreConnector.getTimesStarted(), is(3));
      return true;
    });
  }

  private void startFlow(String flowName) throws Exception {
    ((Flow) getFlowConstruct(flowName)).start();
  }
}
