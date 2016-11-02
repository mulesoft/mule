/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.tck.testmodels.mule.TestTransaction;

import org.junit.Test;

public class LaxAsyncInterceptingMessageProcessorTestCase extends AsyncInterceptingMessageProcessorTestCase {

  public LaxAsyncInterceptingMessageProcessorTestCase(boolean reactive) {
    super(reactive);
  }

  @Override
  @Test
  public void testProcessRequestResponse() throws Exception {
    assertSync(messageProcessor, testEvent());
  }

  @Override
  @Test
  public void testProcessOneWayWithTx() throws Exception {
    Transaction transaction = new TestTransaction(muleContext);
    TransactionCoordination.getInstance().bindTransaction(transaction);

    try {
      assertSync(messageProcessor, testEvent());
    } finally {
      TransactionCoordination.getInstance().unbindTransaction(transaction);
    }
  }

  @Override
  @Test
  public void testProcessRequestResponseWithTx() throws Exception {
    Transaction transaction = new TestTransaction(muleContext);
    TransactionCoordination.getInstance().bindTransaction(transaction);

    try {
      assertSync(messageProcessor, testEvent());
    } finally {
      TransactionCoordination.getInstance().unbindTransaction(transaction);
    }
  }

  @Override
  protected AsyncInterceptingMessageProcessor createAsyncInterceptingMessageProcessor(Processor listener)
      throws Exception {
    LaxAsyncInterceptingMessageProcessor mp = new LaxAsyncInterceptingMessageProcessor();
    mp.setScheduler(scheduler);
    mp.setMuleContext(muleContext);
    mp.setFlowConstruct(getTestFlow(muleContext));
    mp.setListener(listener);
    return mp;
  }

}
