/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms.integration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.compatibility.transport.jms.JmsConstants;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.util.StringUtils;

import org.junit.Test;

/**
 * see EE-1688/MULE-3059
 */
public class JmsSynchronousResponseTestCase extends AbstractJmsFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "integration/jms-synchronous-response.xml";
  }

  @Test
  public void testResponseWithoutReplyTo() throws Exception {
    MuleClient client = muleContext.getClient();

    InternalMessage response = client.send("out1", "TEST_MESSAGE", null).getRight();
    assertNotNull(response);
    assertTrue("Response is not a JMS Message", response.getPayload().getValue() instanceof javax.jms.Message);
    assertJmsMessageIdPresent(response);
  }

  @Test
  public void testResponseWithoutReplyToEndpointProperties() throws Exception {
    MuleClient client = muleContext.getClient();

    InternalMessage response = client.send("out2", "TEST_MESSAGE", null).getRight();
    assertNotNull(response);
    assertTrue("Response is not a JMS Message", response.getPayload().getValue() instanceof javax.jms.Message);
    assertJmsMessageIdPresent(response);
  }

  @Test
  public void testResponseWithReplyTo() throws Exception {
    MuleClient client = muleContext.getClient();

    InternalMessage response = client.send("out3", "TEST_MESSAGE", null).getRight();
    assertNotNull(response);
    assertFalse("Response should not be NullPayload", response.getPayload().getValue() == null);
  }

  private void assertJmsMessageIdPresent(InternalMessage message) {
    String messageId = message.getInboundProperty(JmsConstants.JMS_MESSAGE_ID);
    assertTrue("JMSMessageID is missing", StringUtils.isNotBlank(messageId));
  }
}
