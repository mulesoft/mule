/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.transaction.TransactionException;
import org.mule.runtime.core.exception.DefaultMessagingExceptionStrategy;
import org.mule.runtime.core.transaction.TransactionCoordination;

import org.junit.Test;

/**
 * When exception strategies are used with transactions it should be possible to send the exception message while rolling back the
 * transaction. See MULE-4338
 */
public class ExceptionStrategyTransactionTestCase extends FunctionalTestCase {

  private static String failure;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-strategy-transaction-test-flow.xml";
  }

  @Test
  public void testRequestReply() throws Exception {
    MuleClient client = muleContext.getClient();
    client.dispatch("InputQueueClient", "payload", null);

    // There should be a message on ExceptionQueue
    assertThat(client.request("ExceptionQueue", 10000).getRight().isPresent(), is(true));

    if (failure != null) {
      fail(failure);
    }
  }

  @Test
  public void testNoInfiniteLoop() throws Exception {
    MuleClient client = muleContext.getClient();
    client.dispatch("InputQueueClient2", "payload", null);

    Thread.sleep(500);

    if (failure != null) {
      fail(failure);
    }

  }

  public static class AssertRollbackServiceExceptionStrategy extends DefaultMessagingExceptionStrategy {

    private int visits = 0;

    @Override
    protected MuleEvent routeException(MuleEvent event, FlowConstruct flow, Throwable t) {
      MuleEvent result = super.routeException(event, flow, t);

      if (visits++ > 1) {
        failure = "Exception strategy should only be called once";
        fail("Exception strategy should only be called once");
      }

      try {
        if (TransactionCoordination.getInstance().getTransaction() != null
            && !TransactionCoordination.getInstance().getTransaction().isRollbackOnly()) {
          failure = "transaction should have been set for rollback";
        }
      } catch (TransactionException e) {
        failure = e.getMessage();
        fail(e.getMessage());
      }

      return result;
    }
  }
}
