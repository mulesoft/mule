/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static java.lang.Thread.currentThread;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.TestLegacyMessageUtils.getInboundProperty;
import static org.mule.functional.junit4.TestLegacyMessageUtils.getOutboundProperty;
import org.mule.functional.junit4.TestLegacyMessageBuilder;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.AbstractIntegrationTestCase;

import com.eaio.uuid.UUID;

import org.junit.Test;

public class FlowAsyncBeforeAfterOutboundTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/flow-async-before-after-outbound.xml";
  }

  @Test
  public void testAsyncBefore() throws Exception {
    MuleClient client = muleContext.getClient();

    Message msgSync = flowRunner("test-async-block-before-outbound").withPayload("message").run().getMessage();
    Message msgAsync = client.request("test://test.before.async.out", RECEIVE_TIMEOUT).getRight().get();
    Message msgOut = client.request("test://test.before.out", RECEIVE_TIMEOUT).getRight().get();

    assertCorrectThreads(msgSync, msgAsync, msgOut);
  }

  @Test
  public void testAsyncAfter() throws Exception {
    MuleClient client = muleContext.getClient();

    Message msgSync = flowRunner("test-async-block-after-outbound").withPayload("message").run().getMessage();
    Message msgAsync = client.request("test://test.after.async.out", RECEIVE_TIMEOUT).getRight().get();
    Message msgOut = client.request("test://test.after.out", RECEIVE_TIMEOUT).getRight().get();

    assertCorrectThreads(msgSync, msgAsync, msgOut);
  }

  private void assertCorrectThreads(Message msgSync, Message msgAsync, Message msgOut) throws Exception {
    assertThat(msgSync, not(nullValue()));
    assertThat(msgAsync, not(nullValue()));
    assertThat(msgOut, not(nullValue()));

    assertThat(getInboundProperty(msgOut, "request-response-thread"),
               equalTo(getInboundProperty(msgSync, "request-response-thread")));
    assertThat(getOutboundProperty(msgSync, "request-response-thread"),
               not(equalTo(getOutboundProperty(msgAsync, "async-thread"))));
    assertThat(getOutboundProperty(msgOut, "request-response-thread"),
               not(equalTo(getOutboundProperty(msgAsync, "async-thread"))));
  }

  public static class ThreadSensingMessageProcessor implements Processor {

    private static final ThreadLocal<String> taskTokenInThread = new ThreadLocal<>();

    @Override
    public Event process(Event event) throws MuleException {
      String requestTaskToken;
      if (taskTokenInThread.get() != null) {
        requestTaskToken = taskTokenInThread.get();
      } else {
        requestTaskToken = generateTaskToken();
        taskTokenInThread.set(requestTaskToken);
      }

      return Event.builder(event).message(new TestLegacyMessageBuilder(event.getMessage())
          .addOutboundProperty((String) event.getVariable("property-name").getValue(), requestTaskToken).build()).build();
    }

    protected String generateTaskToken() {
      return currentThread().getName() + " - " + new UUID().toString();
    }
  }
}
