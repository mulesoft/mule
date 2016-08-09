/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

public class TwoEndpointsSinglePortTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "two-endpoints-single-port-flow.xml";
  }

  @Test
  public void testSendToEach() throws Exception {
    sendWithResponse("mycomponent1", "test", "mycomponent1", 10);
    sendWithResponse("mycomponent2", "test", "mycomponent2", 10);
  }

  @Test
  public void testSendToEachWithBadEndpoint() throws Exception {
    MuleClient client = muleContext.getClient();

    sendWithResponse("mycomponent1", "test", "mycomponent1", 5);
    sendWithResponse("mycomponent2", "test", "mycomponent2", 5);

    String url = String.format("http://localhost:%d/mycomponent-notfound", port1.getNumber());
    MuleMessage result = client.send(url, "test", null);
    assertNotNull(result);
    assertNotNull(result.getExceptionPayload());
    final int status = result.getInboundProperty("http.status", 0);
    assertEquals(404, status);

    // Test that after the exception the endpoints still receive events
    sendWithResponse("mycomponent1", "test", "mycomponent1", 5);
    sendWithResponse("mycomponent2", "test", "mycomponent2", 5);
  }

  protected void sendWithResponse(String flowName, String message, String response, int noOfMessages) throws Exception {
    MuleClient client = muleContext.getClient();

    List<Object> results = new ArrayList<Object>();
    for (int i = 0; i < noOfMessages; i++) {
      results.add(getPayloadAsBytes(client
          .send(((InboundEndpoint) ((Flow) muleContext.getRegistry().lookupObject(flowName)).getMessageSource()).getAddress(),
                message, null)));
    }

    assertEquals(noOfMessages, results.size());
    for (int i = 0; i < noOfMessages; i++) {
      assertEquals(response, new String((byte[]) results.get(i)));
    }
  }
}
