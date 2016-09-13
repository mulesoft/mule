/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms.integration;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.exception.DefaultMessagingExceptionStrategy;
import org.mule.runtime.core.util.concurrent.Latch;

import org.hamcrest.core.IsNull;
import org.junit.Test;

public class JmsExceptionStrategyTestCase extends AbstractJmsFunctionalTestCase {

  public static final String MESSAGE = "some message";
  public static final int TIMEOUT = 3000;
  public static final int SHORT_TIMEOUT = 500;
  private Latch latch;
  private MuleClient muleClient;
  private static final long LATCH_AWAIT_TIMEOUT = 3000;

  @Override
  protected String getConfigFile() {
    return "integration/jms-exception-strategy.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    latch = new Latch();
    muleClient = muleContext.getClient();
    DefaultMessagingExceptionStrategy exceptionStrategy = (DefaultMessagingExceptionStrategy) muleContext.getRegistry()
        .lookupFlowConstruct("flowWithoutExceptionStrategyAndTx").getExceptionListener();
    exceptionStrategy.getMessageProcessors().add(new Processor() {

      @Override
      public Event process(Event event) throws MuleException {
        latch.countDown();
        return event;
      }
    });
  }

  @Test
  public void testInExceptionDoRollbackJmsTx() throws Exception {
    muleClient = muleContext.getClient();

    // make sure that target queue is empty to avoid flackyness
    consumeAllItemsInQueue("out");

    muleClient.dispatch("jms://in", MESSAGE, null);
    latch.await(LATCH_AWAIT_TIMEOUT, MILLISECONDS);
    // Stop flow to not consume message again
    Flow flow = muleContext.getRegistry().get("flowWithoutExceptionStrategyAndTx");
    flow.stop();
    // Check message rollback
    // Seems not to be a rollback
    InternalMessage muleMessage = muleClient.request("jms://in", TIMEOUT).getRight().get();
    assertThat(muleMessage, IsNull.<Object>notNullValue());
    // This is currently expected
    /*
     * assertThat(muleMessage, notNullValue()); assertThat((String) muleMessage.getPayload().getValue(), Is.is(MESSAGE));
     */

    // Check outbound-endpoint was not executed
    assertThat(muleClient.request("jms://out", SHORT_TIMEOUT).getRight().isPresent(), is(false));
  }

  @Test
  public void testInExceptionDoRollbackJmsNoTx() throws Exception {
    muleClient = muleContext.getClient();
    muleClient.dispatch("jms://in2", MESSAGE, null);
    latch.await(LATCH_AWAIT_TIMEOUT, MILLISECONDS);
    // Stop flow to not consume message again
    Flow flow = muleContext.getRegistry().get("flowWithoutExceptionStrategyAndNoTx");
    flow.stop();
    // Check message was no consumed
    InternalMessage muleMessage = muleClient.request("jms://in2", TIMEOUT).getRight().get();
    assertThat(muleMessage, IsNull.<Object>notNullValue());

    // Check outbound-endpoint was not executed
    assertThat(muleClient.request("jms://out2", SHORT_TIMEOUT).getRight().isPresent(), is(false));
  }

  @Test
  public void testDefaultStrategyConfigured() throws Exception {
    muleClient = muleContext.getClient();
    muleClient.dispatch("jms://in3", MESSAGE, null);
    latch.await(LATCH_AWAIT_TIMEOUT, MILLISECONDS);
    // Stop flow to not consume message again
    Flow flow = muleContext.getRegistry().get("flowWithDefaultStrategyConfigured");
    flow.stop();
    // Check message was no consumed
    InternalMessage muleMessage = muleClient.request("jms://in3", TIMEOUT).getRight().get();
    assertThat(muleMessage, IsNull.<Object>notNullValue());

    // Check outbound-endpoint was not executed
    assertThat(muleClient.request("jms://out3", SHORT_TIMEOUT).getRight().isPresent(), is(false));
  }

  @Test
  public void testSendExceptionNofication() throws Exception {
    muleClient = muleContext.getClient();
    muleClient.dispatch("jms://in4", MESSAGE, null);
    latch.await(LATCH_AWAIT_TIMEOUT, MILLISECONDS);
    // Stop flow to not consume message again
    Flow flow = muleContext.getRegistry().get("flowWithExceptionNotification");
    flow.stop();
    // Check message was no consumed
    InternalMessage muleMessage = muleClient.request("jms://in4", TIMEOUT).getRight().get();
    assertThat(muleMessage, IsNull.<Object>notNullValue());

    // Check exception notification was sent
    InternalMessage exceptionMessage = muleClient.request("jms://exception4", TIMEOUT).getRight().get();
    assertThat(exceptionMessage, IsNull.<Object>notNullValue());
    assertThat(exceptionMessage.getPayload().getValue(), IsNull.<Object>notNullValue());

    // Check outbound-endpoint was not executed
    assertThat(muleClient.request("jms://out4", SHORT_TIMEOUT).getRight().isPresent(), is(false));
  }

  @Test
  public void testFlowConfiguredForDeadLetterQueue() throws Exception {
    muleClient = muleContext.getClient();
    muleClient.dispatch("jms://in5", MESSAGE, null);
    latch.await(LATCH_AWAIT_TIMEOUT, MILLISECONDS);
    // Stop flow to not consume message again
    Flow flow = muleContext.getRegistry().get("flowConfiguredForDeadLetterQueue");
    flow.stop();
    // Check message was no consumed
    assertThat(muleClient.request("jms://in5", TIMEOUT).getRight().isPresent(), is(false));

    // Check exception notification was sent
    InternalMessage deadLetter = muleClient.request("jms://DLQ5", TIMEOUT).getRight().get();
    assertThat(deadLetter, IsNull.<Object>notNullValue());
    assertThat(deadLetter.getPayload().getValue(), IsNull.<Object>notNullValue());

    // Check outbound-endpoint was not executed
    assertThat(muleClient.request("jms://out5", SHORT_TIMEOUT).getRight().isPresent(), is(false));
  }

  @Test
  public void testFlowConfiguredForDeadLetterQueueTx() throws Exception {
    muleClient = muleContext.getClient();
    muleClient.dispatch("jms://in6", MESSAGE, null);
    latch.await(LATCH_AWAIT_TIMEOUT, MILLISECONDS);
    // Stop flow to not consume message again
    Flow flow = muleContext.getRegistry().get("flowConfiguredForDeadLetterQueueTx");
    flow.stop();
    // Check message was no consumed
    assertThat(muleClient.request("jms://in6", TIMEOUT).getRight().isPresent(), is(false));

    // Check exception notification was sent
    InternalMessage deadLetter = muleClient.request("jms://DLQ6", TIMEOUT).getRight().get();
    assertThat(deadLetter, IsNull.<Object>notNullValue());
    assertThat(deadLetter.getPayload().getValue(), IsNull.<Object>notNullValue());

    // Check outbound-endpoint was not executed
    assertThat(muleClient.request("jms://out6", SHORT_TIMEOUT).getRight().isPresent(), is(false));
  }

  private void consumeAllItemsInQueue(String queue) throws Exception {
    while (muleContext.getClient().request("jms://" + queue, SHORT_TIMEOUT).getRight().isPresent()) {
      // read and discard
    }
  }
}
