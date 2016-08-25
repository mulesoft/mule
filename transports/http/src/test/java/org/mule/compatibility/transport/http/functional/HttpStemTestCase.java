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
import org.mule.functional.functional.EventCallback;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class HttpStemTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "http-stem-test.xml";
  }

  @Test
  public void testStemMatching() throws Exception {
    MuleClient client = muleContext.getClient();
    int port = dynamicPort.getNumber();
    doTest(client, "http://localhost:" + port + "/foo", "/foo", "/foo");
    doTest(client, "http://localhost:" + port + "/foo/baz", "/foo", "/foo/baz");
    doTest(client, "http://localhost:" + port + "/bar", "/bar", "/bar");
    doTest(client, "http://localhost:" + port + "/bar/baz", "/bar", "/bar/baz");
  }

  protected void doTest(MuleClient client, final String url, final String contextPath, final String requestPath)
      throws Exception {
    FunctionalTestComponent testComponent = (FunctionalTestComponent) getComponent(contextPath);
    assertNotNull(testComponent);

    EventCallback callback = (context, component, muleContext) -> {
      MuleMessage msg = context.getMessage();
      assertEquals(requestPath, msg.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY));
      assertEquals(requestPath, msg.getInboundProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY));
      assertEquals(contextPath, msg.getInboundProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY));
    };

    testComponent.setEventCallback(callback);

    MuleMessage result = client.send(url, "Hello World", null).getRight();
    assertEquals("Hello World Received", getPayloadAsString(result));
    final int status = result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0);
    assertEquals(200, status);
  }
}
