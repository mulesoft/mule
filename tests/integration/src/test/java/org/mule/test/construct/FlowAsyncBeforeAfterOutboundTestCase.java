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
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.processor.MessageProcessor;

import org.junit.Test;

public class FlowAsyncBeforeAfterOutboundTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/flow-async-before-after-outbound.xml";
  }

  @Test
  public void testAsyncBefore() throws Exception {
    MuleClient client = muleContext.getClient();

    MuleMessage msgSync = flowRunner("test-async-block-before-outbound").withPayload("message").run().getMessage();

    MuleMessage msgAsync = client.request("test://test.before.async.out", RECEIVE_TIMEOUT).getRight().get();
    MuleMessage msgOut = client.request("test://test.before.out", RECEIVE_TIMEOUT).getRight().get();

    assertCorrectThreads(msgSync, msgAsync, msgOut);

  }

  @Test
  public void testAsyncAfter() throws Exception {
    MuleClient client = muleContext.getClient();

    MuleMessage msgSync = flowRunner("test-async-block-after-outbound").withPayload("message").run().getMessage();

    MuleMessage msgAsync = client.request("test://test.after.async.out", RECEIVE_TIMEOUT).getRight().get();
    MuleMessage msgOut = client.request("test://test.after.out", RECEIVE_TIMEOUT).getRight().get();

    assertCorrectThreads(msgSync, msgAsync, msgOut);
  }

  private void assertCorrectThreads(MuleMessage msgSync, MuleMessage msgAsync, MuleMessage msgOut) throws Exception {
    assertThat(msgSync, not(nullValue()));
    assertThat(msgAsync, not(nullValue()));
    assertThat(msgOut, not(nullValue()));

    assertThat(msgOut.getInboundProperty("request-response-thread"),
               equalTo(msgSync.getInboundProperty("request-response-thread")));
    assertThat(msgSync.getOutboundProperty("request-response-thread"),
               not(equalTo(msgAsync.getOutboundProperty("async-thread"))));
    assertThat(msgOut.getOutboundProperty("request-response-thread"), not(equalTo(msgAsync.getOutboundProperty("async-thread"))));
  }

  public static class ThreadSensingMessageProcessor implements MessageProcessor {

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      event.setMessage(MuleMessage.builder(event.getMessage())
          .addOutboundProperty(event.getFlowVariable("property-name"), currentThread().getName()).build());
      return event;
    }
  }
}
