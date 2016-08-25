/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Test;

public class DynamicEndpointWithConnectorTestCase extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "dynamic-endpoint-with-connector-config.xml";
  }

  @Test
  public void testDynamicEndpointAcceptsConnectorRef() throws Exception {
    MuleClient client = muleContext.getClient();

    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).addOutboundProperty("queueName", "test.out").build();
    MuleMessage test = client.send("vm://input", message).getRight();
    assertNotNull(test);

    MuleMessage response = client.request("jms://test.out", 5000).getRight().get();
    assertEquals(TEST_PAYLOAD, response.getPayload());
  }
}
