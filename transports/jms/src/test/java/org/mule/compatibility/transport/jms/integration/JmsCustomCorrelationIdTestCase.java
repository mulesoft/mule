/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms.integration;

import static org.junit.Assert.assertEquals;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Test;

public class JmsCustomCorrelationIdTestCase extends AbstractJmsFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "integration/jms-custom-correlation.xml";
  }

  @Test
  public void testExplicitReplyToAsyncSet() throws MuleException {
    MuleClient client = muleContext.getClient();
    MuleMessage response =
        client.send("vm://in4",
                    MuleMessage.builder().payload(TEST_MESSAGE).addOutboundProperty("customCorrelation", "abcdefghij").build())
            .getRight();
    // We get the original message back, not the result from the remote component
    assertEquals(TEST_MESSAGE + " TestService1", response.getPayload());
  }
}
