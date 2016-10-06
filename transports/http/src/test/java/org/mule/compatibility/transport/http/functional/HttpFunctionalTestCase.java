/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.compatibility.transport.http.HttpConnector;
import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.functional.functional.EventCallback;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.ClassRule;
import org.junit.Test;

public class HttpFunctionalTestCase extends CompatibilityFunctionalTestCase {

  protected static String TEST_MESSAGE = "Test Http Request (R�dgr�d), 57 = \u06f7\u06f5 in Arabic";
  protected boolean checkPathProperties = true;

  @ClassRule
  public static DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "http-functional-test-flow.xml";
  }

  @Test
  public void testSend() throws Exception {
    FunctionalTestComponent testComponent = getFunctionalTestComponent("testComponent");
    assertNotNull(testComponent);

    if (checkPathProperties) {
      EventCallback callback = (context, component, muleContext) -> {
        InternalMessage msg = context.getMessage();
        assertEquals("/", msg.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY));
        assertEquals("/", msg.getInboundProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY));
        assertEquals("/", msg.getInboundProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY));
      };

      testComponent.setEventCallback(callback);
    }

    MuleClient client = muleContext.getClient();
    InternalMessage result =
        client.send("clientEndpoint",
                    InternalMessage.builder().payload(TEST_MESSAGE).mediaType(MediaType.parse("text/plain;charset=UTF-8"))
                        .build())
            .getRight();
    assertEquals(TEST_MESSAGE + " Received", getPayloadAsString(result));
  }
}
