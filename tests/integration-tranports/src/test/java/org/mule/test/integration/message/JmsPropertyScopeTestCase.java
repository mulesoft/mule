/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_REPLY_TO_PROPERTY;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Test;

public class JmsPropertyScopeTestCase extends AbstractPropertyScopeTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/message/jms-property-scope-flow.xml";
  }

  @Override
  @Test
  public void testRequestResponse() throws Exception {
    MuleClient client = muleContext.getClient();

    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).addOutboundProperty("foo", "fooValue")
        .addOutboundProperty(MULE_REPLY_TO_PROPERTY, "jms://reply").build();

    client.dispatch("inbound", message);
    MuleMessage result = client.request("jms://reply", 10000).getRight().get();

    assertNotNull(result);
    assertEquals("test bar", result.getPayload());
    assertEquals("fooValue", result.getInboundProperty("foo"));
  }
}
