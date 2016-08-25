/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_REPLY_TO_PROPERTY;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Test;

public class InOutAsyncTestCase extends FunctionalTestCase {

  public static final long TIMEOUT = 3000;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/messaging/meps/pattern_In-Out-Async-flow.xml";
  }

  @Test
  public void testExchange() throws Exception {
    MuleClient client = muleContext.getClient();

    MuleMessage result = client.send("inboundEndpoint",
                                     MuleMessage.builder().payload("some data")
                                         // Almost any endpoint can be used here
                                         .addOutboundProperty(MULE_REPLY_TO_PROPERTY, "jms://client-reply").build())
        .getRight();
    assertNotNull(result);
    assertEquals("got it!", getPayloadAsString(result));

    final Object foo = result.getInboundProperty("foo");
    assertNotNull(foo);
    assertEquals("bar", foo);
  }
}
