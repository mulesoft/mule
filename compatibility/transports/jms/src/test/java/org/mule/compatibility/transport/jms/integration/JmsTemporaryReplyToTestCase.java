/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms.integration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_REPLY_TO_PROPERTY;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;

import java.nio.charset.Charset;

import org.junit.Ignore;
import org.junit.Test;

/**
 * TODO this test does not use the Test scenarios, I think it would need a new Method sendAndReceive It might make sense to leave
 * this test as is because it tests that the client also works with ReplyTo correctly
 */
public class JmsTemporaryReplyToTestCase extends AbstractJmsFunctionalTestCase {

  private static String ECHO_FLOW_NAME = "EchoFlow";

  @Override
  protected String getConfigFile() {
    return "integration/jms-temporary-replyTo.xml";
  }

  @Test
  public void testReplyEnabledSync() throws Exception {
    InternalMessage response = flowRunner("JMSService1SyncFixed").withPayload(TEST_MESSAGE).run().getMessage();
    assertEchoResponse(response);
  }

  @Test
  public void testReplyEnabledSyncTimeout() throws Exception {
    InternalMessage response = flowRunner("JMSService1SyncTimeoutFixed").withPayload(TEST_MESSAGE).run().getMessage();
    assertNullPayloadResponse(response);
  }

  @Test
  public void testTemporaryReplyEnabledSync() throws Exception {
    InternalMessage response = flowRunner("JMSService1Sync").withPayload(TEST_MESSAGE).run().getMessage();
    assertEchoResponse(response);
  }

  @Test
  public void testTemporaryReplyEnabledSyncTimeout() throws Exception {
    InternalMessage response = flowRunner("JMSService1SyncTimeout").withPayload(TEST_MESSAGE).run().getMessage();
    assertNullPayloadResponse(response);
  }

  @Test
  public void testTemporaryReplyDisabledSync() throws Exception {
    // TODO This behaviour appears inconsistent. NullPayload would be more appropriate.
    assertThat(flowRunner("JMSService2Sync").run(), is(nullValue()));
  }

  @Test
  public void testDisableTemporaryReplyOnTheConnector() throws Exception {
    InternalMessage response = flowRunner("JMSService3").withPayload(TEST_MESSAGE).run().getMessage();
    assertEquals(TEST_MESSAGE, response.getPayload().getValue());
  }

  @Test
  @Ignore("#[mel:context:serviceName] not supported")
  public void testExplicitReplyToAsyncSet() throws Exception {
    InternalMessage response = flowRunner("JMSService4").withPayload(TEST_MESSAGE).run().getMessage();
    // We get the original message back, not the result from the remote component
    assertEquals(TEST_MESSAGE + " TestService1", response.getPayload().getValue());
  }

  private void assertEchoResponse(InternalMessage response) throws Exception {
    assertThat(response.getPayload().getValue(), equalTo(TEST_MESSAGE + " " + ECHO_FLOW_NAME));
  }

  private void assertNullPayloadResponse(InternalMessage response) {
    assertThat(response.getPayload().getValue(), is(nullValue()));
  }

  public static class SetReplyTo extends AbstractMessageTransformer {

    @Override
    public Object transformMessage(Event event, Charset outputEncoding) throws TransformerException {
      return InternalMessage.builder(event.getMessage())
          .addOutboundProperty(MULE_REPLY_TO_PROPERTY, muleContext.getRegistry().get(MIDDLE_ENDPOINT_KEY)).build();
    }
  }
}
