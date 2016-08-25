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
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;

import java.nio.charset.Charset;

import org.junit.Test;

public class ReplyToChainIntegration3TestCase extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/replyto/replyto-chain-integration-test-3.xml";
  }

  @Test
  public void testReplyToChain() throws Exception {
    String message = "test";

    MuleClient client = muleContext.getClient();
    client.dispatch("vm://pojo1", message, null);
    MuleMessage result = client.request("jms://response", 10000).getRight().get();
    assertNotNull(result);
    assertEquals("Received: " + message, result.getPayload());
  }

  public static class SetReplyTo extends AbstractMessageTransformer {

    @Override
    public Object transformMessage(MuleEvent event, Charset outputEncoding) throws TransformerException {
      final MuleMessage message =
          MuleMessage.builder(event.getMessage()).addOutboundProperty(MULE_REPLY_TO_PROPERTY, "jms://response").build();
      event.setMessage(message);
      return message;
    }
  }
}
