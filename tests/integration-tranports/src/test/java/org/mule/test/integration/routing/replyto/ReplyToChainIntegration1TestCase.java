/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.replyto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_REMOTE_SYNC_PROPERTY;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Test;

public class ReplyToChainIntegration1TestCase extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/replyto/replyto-chain-integration-test-1.xml";
  }

  @Test
  public void testReplyToChain() throws Exception {
    String message = "test";

    MuleClient client = muleContext.getClient();
    MuleMessage result =
        client.send("vm://pojo1",
                    MuleMessage.builder().payload(message).addOutboundProperty(MULE_REMOTE_SYNC_PROPERTY, "false").build())
            .getRight();
    assertNotNull(result);
    assertEquals("Received: " + message, getPayloadAsString(result));
  }

  @Test
  public void testReplyToChainWithoutProps() throws Exception {
    String message = "test";

    MuleClient client = muleContext.getClient();
    MuleMessage result = client.send("vm://pojo1", MuleMessage.builder().payload(message).build()).getRight();
    assertNotNull(result);
    assertEquals("Received: " + message, getPayloadAsString(result));
  }

}
