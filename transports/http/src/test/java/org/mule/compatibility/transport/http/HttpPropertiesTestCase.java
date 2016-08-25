/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.module.http.api.HttpHeaders;
import org.mule.tck.junit4.rule.DynamicPort;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Rule;
import org.junit.Test;

public class HttpPropertiesTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "http-properties-conf.xml";
  }

  @Test
  public void testPropertiesGetMethod() throws Exception {
    GetMethod httpGet = new GetMethod("http://localhost:" + dynamicPort.getNumber() + "/resources/client?id=1");
    new HttpClient().executeMethod(httpGet);
    String result = httpGet.getResponseBodyAsString();
    assertEquals("Retrieving client with id = 1", result);
  }

  @Test
  public void testPropertiesPostMethod() throws Exception {
    MuleClient client = muleContext.getClient();

    MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/resources/client", MuleMessage.builder()
        .payload("name=John&lastname=Galt").mediaType(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED).build()).getRight();

    assertNotNull(response);
    assertEquals("client", response.getInboundProperty("http.relative.path"));
    assertEquals("http://localhost:" + dynamicPort.getNumber() + "/resources", response.getInboundProperty("http.context.uri"));
    assertEquals("Storing client with name = John and lastname = Galt", getPayloadAsString(response));
  }

  @Test
  public void testRedirectionWithRelativeProperty() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage response =
        client.send("http://localhost:" + dynamicPort.getNumber() + "/redirect/products?retrieve=all&order=desc",
                    getTestMuleMessage(TEST_MESSAGE))
            .getRight();
    assertEquals("Successfully redirected: products?retrieve=all&order=desc", getPayloadAsString(response));
  }
}
