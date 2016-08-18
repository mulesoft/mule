/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.functional.functional.FlowAssert.verify;

import org.mule.functional.exceptions.FunctionalTestException;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.runtime.core.component.ComponentException;
import org.mule.runtime.core.message.Correlation;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class NonBlockingNotSupportedFunctionalTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "non-blocking-not-supported-test-config.xml";
  }

  @Test
  public void syncFlow() throws Exception {
    flowRunner("syncFlow").withPayload(TEST_MESSAGE).nonBlocking().run();
  }

  @Test
  public void splitter() throws Exception {
    flowRunner("splitter").withPayload(new String[] {"1", "2", "3"}).nonBlocking().run();
  }

  @Test
  public void untilSuccessful() throws Exception {
    flowRunner("untilSuccessful").withPayload(TEST_MESSAGE).nonBlocking().run();
  }

  @Test
  public void scatterGather() throws Exception {
    flowRunner("scatterGather").withPayload(TEST_MESSAGE).nonBlocking().run();
  }

  @Test
  public void all() throws Exception {
    flowRunner("all").withPayload(TEST_MESSAGE).nonBlocking().run();
  }

  @Test
  public void firstSuccessful() throws Exception {
    flowRunner("firstSuccessful").withPayload(TEST_MESSAGE).nonBlocking().run();
  }

  @Test
  public void roundRobin() throws Exception {
    flowRunner("roundRobin").withPayload(TEST_MESSAGE).nonBlocking().run();
  }

  @Test
  public void requestReply() throws Exception {
    flowRunner("requestReply").withPayload(TEST_MESSAGE).nonBlocking().run();
  }

  @Test
  public void aggregator() throws Exception {
    String correlationId = "id";
    int correlationGroupSize = 3;

    flowRunner("aggregator").withPayload(TEST_MESSAGE).withSourceCorrelationId(correlationId)
        .withCorrelation(new Correlation(correlationId, correlationGroupSize, 1)).nonBlocking().runNoVerify();

    flowRunner("aggregator").withPayload(TEST_MESSAGE).withSourceCorrelationId(correlationId)
        .withCorrelation(new Correlation(correlationId, correlationGroupSize, 2)).nonBlocking().runNoVerify();

    flowRunner("aggregator").withPayload(TEST_MESSAGE).withSourceCorrelationId(correlationId)
        .withCorrelation(new Correlation(correlationId, correlationGroupSize, 3)).nonBlocking().run();
  }

  @Test
  public void poll() throws Exception {
    final Latch latch = new Latch();
    ((FunctionalTestComponent) getComponent("poll")).setEventCallback((context, component, muleContext) -> latch.countDown());
    latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);
    verify("poll");
  }

  @Test
  public void transactional() throws Exception {
    flowRunner("transactional").withPayload(TEST_MESSAGE).nonBlocking().run();
  }

  @Test
  public void rollbackRollbackExceptionStrategy() throws Exception {
    try {
      flowRunner("rollbackRollbackExceptionStrategy").withPayload(TEST_MESSAGE).nonBlocking().run();
      fail("Exception Expected");
    } catch (ComponentException componentException) {
      assertThat(componentException.getCause(), instanceOf(FunctionalTestException.class));
    } finally {
      verify("rollbackRollbackExceptionStrategy");
      verify("rollbackExceptionStrategyChild");
    }
  }

  @Test
  public void catchRollbackExceptionStrategy() throws Exception {
    flowRunner("catchRollbackExceptionStrategy").withPayload(TEST_MESSAGE).nonBlocking().run();
    verify("rollbackExceptionStrategyChild");
  }

}

