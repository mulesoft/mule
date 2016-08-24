/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static java.lang.Thread.currentThread;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.functional.junit4.TransactionConfigEnum;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.tck.testmodels.mule.TestTransactionFactory;

import org.junit.Test;

public class FlowDefaultProcessingStrategyTestCase extends AbstractIntegrationTestCase {

  protected static final String PROCESSOR_THREAD = "processor-thread";
  protected static final String FLOW_NAME = "Flow";

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/flow-default-processing-strategy-config.xml";
  }

  @Test
  public void requestResponse() throws Exception {
    MuleMessage response = flowRunner(FLOW_NAME).withPayload(TEST_PAYLOAD).run().getMessage();
    assertThat(response.getPayload().toString(), is(TEST_PAYLOAD));
    MuleMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();
    assertThat(message.getOutboundProperty(PROCESSOR_THREAD), is(Thread.currentThread().getName()));
  }

  @Test
  public void oneWay() throws Exception {
    flowRunner(FLOW_NAME).withPayload(TEST_PAYLOAD).asynchronously().run();
    MuleMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();
    assertThat(message.getOutboundProperty(PROCESSOR_THREAD), is(not(Thread.currentThread().getName())));
  }

  @Test
  public void requestResponseTransacted() throws Exception {
    flowRunner("Flow").withPayload(TEST_PAYLOAD).transactionally(TransactionConfigEnum.ACTION_NONE, new TestTransactionFactory())
        .run();

    MuleMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();
    assertThat(message.getOutboundProperty(PROCESSOR_THREAD), is(Thread.currentThread().getName()));
  }

  @Test
  public void oneWayTransacted() throws Exception {
    flowRunner("Flow").withPayload(TEST_PAYLOAD).transactionally(TransactionConfigEnum.ACTION_NONE, new TestTransactionFactory())
        .asynchronously().run();

    MuleMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();
    assertThat(message.getOutboundProperty(PROCESSOR_THREAD), is(Thread.currentThread().getName()));
  }

  protected void testTransacted(MessageExchangePattern mep) throws Exception {
    flowRunner("Flow").withPayload(TEST_PAYLOAD).transactionally(TransactionConfigEnum.ACTION_NONE, new TestTransactionFactory())
        .run();

    MuleMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();
    assertThat(message.getOutboundProperty(PROCESSOR_THREAD), is(Thread.currentThread().getName()));
  }


  public static class ThreadSensingMessageProcessor implements MessageProcessor {

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      event.setMessage(MuleMessage.builder(event.getMessage()).addOutboundProperty(PROCESSOR_THREAD, currentThread().getName())
          .build());
      return event;
    }
  }

}
