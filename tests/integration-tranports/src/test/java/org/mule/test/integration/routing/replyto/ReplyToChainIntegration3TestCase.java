/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.replyto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_REPLY_TO_PROPERTY;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;

import java.nio.charset.Charset;

import org.junit.Test;

public class ReplyToChainIntegration3TestCase extends CompatibilityFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/replyto/replyto-chain-integration-test-3.xml";
  }

  @Test
  public void testReplyToChain() throws Exception {
    String message = "test";

    MuleClient client = muleContext.getClient();
    client.dispatch("vm://pojo1", message, null);
    InternalMessage result = client.request("jms://response", 10000).getRight().get();
    assertNotNull(result);
    assertEquals("Received: " + message, result.getPayload().getValue());
  }

  public static class SetReplyTo extends AbstractMessageTransformer {

    @Override
    public Object transformMessage(Event event, Charset outputEncoding) throws TransformerException {
      return InternalMessage.builder(event.getMessage()).addOutboundProperty(MULE_REPLY_TO_PROPERTY, "jms://response").build();
    }
  }
}
