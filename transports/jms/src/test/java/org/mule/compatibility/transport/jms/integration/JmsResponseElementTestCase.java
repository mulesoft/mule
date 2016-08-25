/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms.integration;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_REPLY_TO_PROPERTY;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Test;

public class JmsResponseElementTestCase extends FunctionalTestCase {

  public static final String MESSAGE = "A Message";
  public static final String EXPECTED_MODIFIED_MESSAGE = "A Message jms flow content";
  public static final int TIMEOUT = 3000;
  public static final int TINY_TIMEOUT = 300;

  @Override
  protected String getConfigFile() {
    return "integration/jms-response-element-config-flow.xml";
  }

  @Test
  public void testOutboundEndpointResponse() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage response = client.send("vm://vminbound", MuleMessage.builder().payload("some message").build()).getRight();
    assertThat(getPayloadAsString(response), is(EXPECTED_MODIFIED_MESSAGE));
    assertThat(response.getInboundProperty("test"), Is.is("test"));
  }

  @Test
  public void testInboundEndpointResponse() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage response = client.send("vm://vminbound2", MuleMessage.builder().payload(MESSAGE).build()).getRight();
    assertThat(getPayloadAsString(response), is(EXPECTED_MODIFIED_MESSAGE));
  }

  @Test
  public void testInboundEndpointResponseWithReplyTo() throws Exception {
    MuleClient client = muleContext.getClient();

    String replyToUri = "jms://out2";
    client.dispatch("jms://out",
                    MuleMessage.builder().payload(MESSAGE).addOutboundProperty(MULE_REPLY_TO_PROPERTY, replyToUri).build());

    MuleMessage response = client.request(replyToUri, TIMEOUT).getRight().get();
    assertThat(getPayloadAsString(response), is(EXPECTED_MODIFIED_MESSAGE));
    assertThat(client.request(replyToUri, TINY_TIMEOUT).getRight().isPresent(), is(false));
  }

  @Test
  public void testInboundEndpointOneWay() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage response = client.send("jms://in3", MuleMessage.builder().payload(MESSAGE).build()).getRight();
    assertThat(getPayloadAsString(response), is(EXPECTED_MODIFIED_MESSAGE));
  }
}
