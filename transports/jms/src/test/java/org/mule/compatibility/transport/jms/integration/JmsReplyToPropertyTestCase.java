/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

public class JmsReplyToPropertyTestCase extends AbstractJmsFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "integration/jms-replyto-property.xml";
  }

  @Test
  @Ignore("MULE-9628")
  public void testReplyTo() throws Exception {
    MuleClient client = muleContext.getClient();
    Map<String, Serializable> props = new HashMap<>();
    props.put("JMSReplyTo", "middle");
    client.dispatch("in", DEFAULT_INPUT_MESSAGE, props);

    // Check that the property is still on the outbound message
    MuleMessage output = client.request("out", 2000).getRight().get();
    assertNotNull(output);
    final Object o = output.getOutboundProperty("JMSReplyTo");
    assertTrue(o.toString().contains("middle"));

    // Check that the reply message was generated
    output = client.request("middle", 2000).getRight().get();
    assertNotNull(output);
    assertEquals(DEFAULT_OUTPUT_MESSAGE, output.getPayload());
  }
}
