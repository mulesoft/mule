/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.lifecycle.Callable;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Ignore;
import org.junit.Test;

/*
EE-2430 - Check that JMS SubReceiver executes concurrently.
This test that each SubReceiver is using a different Session since
if they are using the same Session activeMQ won't execute them concurrently
(since it will not do two onMessage invocations concurrently using the same session)
One of the latch.await(..) will fail in that case.
 */
public class JmsConcurrentConsumerExecutionTestCase extends FunctionalTestCase {

  public static final String MESSAGE = "some message";
  public static final int TIMEOUT = 10000;
  private static final Latch messageSuccessfulReceived = new Latch();
  private static final Latch messageFailureReceived = new Latch();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/transaction/jms-concurrent-in-transaction.xml";
  }

  @Test
  @Ignore("MULE-6926")
  public void testTwoMessagesOneRollbackOneCommit() throws Exception {
    MuleClient muleClient = muleContext.getClient();
    muleClient.dispatch("jms://in", "success", null);
    muleClient.dispatch("jms://in", "failure", null);
    if (!messageSuccessfulReceived.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("JMS messages didn't execute concurrently, might be using only one Session for more than one transaction");
    }
    if (!messageFailureReceived.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("JMS messages didn't execute concurrently, might be using only one Session for more than one transaction");
    }
    Flow flowWithTxConfigured = (Flow) getFlowConstruct("flowWithTxConfigured");
    flowWithTxConfigured.stop();
    InternalMessage muleMessage = muleClient.request("jms://in", TIMEOUT).getRight().get();
    assertThat(muleMessage, IsNull.<Object>notNullValue());
    assertThat(muleClient.request("jms://in", TIMEOUT).getRight().isPresent(), is(false));
  }

  public static class SuccessComponent implements Callable {

    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception {
      messageSuccessfulReceived.release();
      messageFailureReceived.await(TIMEOUT, TimeUnit.MILLISECONDS);
      return eventContext.getMessage();
    }
  }

  public static class FailureComponent implements Callable {

    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception {
      try {
        throw new RuntimeException("something bad happend :)");
      } finally {
        messageFailureReceived.release();
        messageSuccessfulReceived.await(TIMEOUT, TimeUnit.MILLISECONDS);
      }
    }
  }
}
