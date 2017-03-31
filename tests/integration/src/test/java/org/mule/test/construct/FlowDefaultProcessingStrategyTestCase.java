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
import static org.mule.functional.junit4.TransactionConfigEnum.ACTION_ALWAYS_BEGIN;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.test.AbstractIntegrationTestCase;

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
    Message response = flowRunner(FLOW_NAME).withPayload(TEST_PAYLOAD).run().getMessage();
    assertThat(response.getPayload().getValue().toString(), is(TEST_PAYLOAD));
    Message message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();

    assertThat(((InternalMessage) message).getOutboundProperty(PROCESSOR_THREAD), is(not(currentThread().getName())));
  }

  @Test
  public void oneWay() throws Exception {
    flowRunner(FLOW_NAME).withPayload(TEST_PAYLOAD).run();
    Message message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();

    assertThat(((InternalMessage) message).getOutboundProperty(PROCESSOR_THREAD), is(not(currentThread().getName())));
  }

  @Test
  public void requestResponseTransacted() throws Exception {
    flowRunner("Flow").withPayload(TEST_PAYLOAD).transactionally(ACTION_ALWAYS_BEGIN, new TestTransactionFactory())
        .run();

    Message message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();

    assertThat(((InternalMessage) message).getOutboundProperty(PROCESSOR_THREAD), is(currentThread().getName()));
  }

  @Test
  public void oneWayTransacted() throws Exception {
    flowRunner("Flow").withPayload(TEST_PAYLOAD).transactionally(ACTION_ALWAYS_BEGIN, new TestTransactionFactory())
        .run();

    Message message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();

    assertThat(((InternalMessage) message).getOutboundProperty(PROCESSOR_THREAD), is(currentThread().getName()));
  }

  public static class ThreadSensingMessageProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      return Event.builder(event)
          .message(InternalMessage.builder(event.getMessage()).addOutboundProperty(PROCESSOR_THREAD, currentThread().getName())
              .build())
          .build();
    }
  }

}
