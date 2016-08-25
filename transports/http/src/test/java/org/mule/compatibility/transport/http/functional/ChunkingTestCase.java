/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static org.junit.Assert.assertEquals;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.transport.http.HttpConnector;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class ChunkingTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "chunking-test.xml";
  }

  @Test
  public void testPartiallyReadRequest() throws Exception {
    MuleClient client = muleContext.getClient();

    byte[] msg = new byte[100 * 1024];

    MuleMessage result =
        client.send(((InboundEndpoint) ((Flow) muleContext.getRegistry().lookupObject("/foo")).getMessageSource()).getAddress(),
                    msg, null)
            .getRight();
    assertEquals("Hello", getPayloadAsString(result));
    int status = result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0);
    assertEquals(200, status);

    result =
        client.send(((InboundEndpoint) ((Flow) muleContext.getRegistry().lookupObject("/foo")).getMessageSource()).getAddress(),
                    msg, null)
            .getRight();
    assertEquals("Hello", getPayloadAsString(result));
    status = result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0);
    assertEquals(200, status);
  }
}
