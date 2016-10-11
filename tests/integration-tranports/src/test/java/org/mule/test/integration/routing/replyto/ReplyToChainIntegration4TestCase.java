/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.replyto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;

import org.junit.Test;

public class ReplyToChainIntegration4TestCase extends CompatibilityFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/replyto/replyto-chain-integration-test-4.xml";
  }

  @Test
  public void testReplyToChain() throws Exception {
    String message = "test";

    MuleClient client = muleContext.getClient();
    InternalMessage result = client.send("vm://pojo1", message, null).getRight();
    assertNotNull(result);
    assertEquals("Received: " + message, result.getPayload().getValue());
  }
}
