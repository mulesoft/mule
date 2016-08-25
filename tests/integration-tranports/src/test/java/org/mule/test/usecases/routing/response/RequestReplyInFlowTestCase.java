/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.routing.response;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Test;

/**
 * Test the request-reply construct in flows
 */
public class RequestReplyInFlowTestCase extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/usecases/routing/response/request-reply-flow.xml";
  }

  @Test
  public void testRequestReply() throws Exception {
    MuleClient client = muleContext.getClient();
    client.dispatch(getDispatchUrl(), MuleMessage.builder().payload("Message went").build());
    MuleMessage reply = client.request(getDestinationUrl(), 10000).getRight().get();
    assertNotNull(reply);
    assertEquals("Message went-out-and-back-in", reply.getPayload());
  }

  protected String getDispatchUrl() {
    return "vm://input";
  }

  protected String getDestinationUrl() {
    return "vm://destination";
  }
}
